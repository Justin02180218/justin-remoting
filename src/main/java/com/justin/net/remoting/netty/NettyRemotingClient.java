package com.justin.net.remoting.netty;

import com.justin.net.remoting.InvokeCallback;
import com.justin.net.remoting.RemotingClient;
import com.justin.net.remoting.common.RemotingUtil;
import com.justin.net.remoting.netty.conf.NettyClientConfig;
import com.justin.net.remoting.netty.event.NettyEventListener;
import com.justin.net.remoting.netty.handler.NettyClientConnMgrHandler;
import com.justin.net.remoting.netty.handler.NettyClientHandler;
import com.justin.net.remoting.netty.handler.NettyDecoder;
import com.justin.net.remoting.netty.handler.NettyEncoder;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyRemotingClient extends NettyRemotingAbstract implements RemotingClient {
    private static final Logger logger = LogManager.getLogger(NettyRemotingClient.class.getSimpleName());
    private static final long LOCK_TIMEOUT = 3000;

    private final NettyClientConfig clientConfig;
    private final Bootstrap bootstrap;
    private final EventLoopGroup selector;

    private final Lock lockChannelTables = new ReentrantLock();
    private final ConcurrentMap<String, ChannelWrapper> channelTables = new ConcurrentHashMap<String, ChannelWrapper>();

    private final Timer timer = new Timer("ClientHouseKeepingService", true);

    private final ExecutorService publicExecutor;
    private final NettyEventListener eventListener;

    private ExecutorService callbackExecutor;
    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    public NettyRemotingClient(final NettyClientConfig clientConfig) {
        this(clientConfig, null);
    }

    public NettyRemotingClient(final NettyClientConfig clientConfig, final NettyEventListener eventListener) {
        super(clientConfig.getOnewayValue(), clientConfig.getAsyncValue());
        this.bootstrap = new Bootstrap();
        this.clientConfig = clientConfig;
        this.eventListener = eventListener;

        this.publicExecutor = Executors.newFixedThreadPool(clientConfig.getCallbackExecutorThreads(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Client_public_executor_" + index.getAndIncrement());
            }
        });

        this.selector = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Client_selector_" + index.getAndIncrement());
            }
        });
    }

    @Override
    public void start() {
        defaultEventExecutorGroup = new DefaultEventExecutorGroup(clientConfig.getWorkerThreads(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Client_work_" + index);
            }
        });

        bootstrap.group(selector).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, clientConfig.getConnectTimeout())
                .option(ChannelOption.SO_SNDBUF, clientConfig.getSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, clientConfig.getSocketRcvBufSize())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(defaultEventExecutorGroup,
                                new NettyEncoder(),
                                new NettyDecoder(),
                                new IdleStateHandler(0, 0, clientConfig.getChannelMaxIdleSeconds()),
                                new NettyClientConnMgrHandler(NettyRemotingClient.this),
                                new NettyClientHandler(NettyRemotingClient.this)
                        );
                    }
                });

        super.start();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    NettyRemotingClient.this.scanResponseTable();
                }catch (Throwable e) {
                    logger.error("Client Scan response table " + e.getMessage());
                }
            }
        }, 1000*3, 1000);
    }

    @Override
    public void shutdown() {
        try {
            if (timer != null) {
                timer.cancel();
            }

            for (ChannelWrapper cw : channelTables.values()) {
                closeChannel(cw.getChannel());
            }
            channelTables.clear();

            selector.shutdownGracefully();

            super.shutdown();

            if (defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }

            if (publicExecutor != null) {
                this.publicExecutor.shutdown();
            }
        }catch (Exception e) {
            logger.error("Client shutdown exception: " + e.getMessage());
        }
    }

    @Override
    public RemotingMessage invokeSync(String addr, RemotingMessage request, long timeout) throws Exception {
        long beginTime = System.currentTimeMillis();
        final Channel channel = this.createChannel(addr);
        if (channel != null && channel.isActive()) {
            try {
                long costTime = System.currentTimeMillis() - beginTime;
                if (costTime > timeout) {
                    throw new Exception("invokeSync call timeout!");
                }

                RemotingMessage response = this.invokeSyncImpl(channel, request, timeout-costTime);
                return response;
            }catch (Exception e) {
                this.closeChannel(channel);
                throw e;
            }
        }else {
            this.closeChannel(channel);
            throw new Exception("Create channel exception " + addr);
        }
    }

    @Override
    public void invokeAsync(String addr, RemotingMessage request, long timeout, InvokeCallback invokeCallback) throws Exception {
        long beginTime = System.currentTimeMillis();
        final Channel channel = createChannel(addr);
        if (channel != null && channel.isActive()) {
            try {
                long costTime = System.currentTimeMillis() - beginTime;
                if (costTime > timeout) {
                    throw new Exception("invokeAsync call timeout!");
                }

                this.invokeAsyncImpl(channel, request, timeout, invokeCallback);
            }catch (Exception e) {
                this.closeChannel(channel);
                throw e;
            }
        }else {
            this.closeChannel(channel);
            throw new Exception("Create channel exception " +addr);
        }
    }

    @Override
    public void invokeOneway(String addr, RemotingMessage request, long timeout) throws Exception {
        final Channel channel = this.createChannel(addr);
        if (channel != null && channel.isActive()) {
            this.invokeOnewayImpl(channel, request, timeout);
        }else {
            this.closeChannel(channel);
            throw new Exception("Create channel exception " + addr);
        }
    }

    @Override
    public NettyEventListener getNettyEventListener() {
        return eventListener;
    }

    @Override
    public void setCallbackExecutor(final ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return callbackExecutor != null ? callbackExecutor : publicExecutor;
    }

    @Override
    public ExecutorService getPublicExecutor() {
        return publicExecutor;
    }

    public void closeChannel(final Channel channel) {
        if (channel == null) {
            return;
        }

        try {
            if (lockChannelTables.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                try {
                    String remoteAddr = null;
                    for (Map.Entry<String, ChannelWrapper> entry : channelTables.entrySet()) {
                        String key = entry.getKey();
                        ChannelWrapper value = entry.getValue();
                        if (value != null && value.getChannel() != null) {
                            if (value.getChannel() == channel) {
                                remoteAddr = key;
                                break;
                            }
                        }
                    }

                    if (remoteAddr != null) {
                        channelTables.remove(remoteAddr);
                        channel.close();
                    }
                }catch(Exception e) {
                    logger.error("Close channel exception: " + e.getMessage());
                }finally {
                    lockChannelTables.unlock();
                }
            }else {
                logger.warn("Close channel timeout: " + LOCK_TIMEOUT);
            }
        }catch (Exception e) {
            logger.error("Close channel exception: " + e.getMessage());
        }
    }

    private Channel createChannel(final String addr) throws Exception {
        ChannelWrapper cw = channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        if (lockChannelTables.tryLock(LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
            try {
                boolean create = false;
                cw = channelTables.get(addr);
                if (cw != null) {
                    if (cw.isOK()) {
                        return cw.getChannel();
                    }else if (!cw.getChannelFuture().isDone()) {
                        create = false;
                    }else {
                        channelTables.remove(addr);
                        create = true;
                    }
                }else {
                    create = true;
                }

                if (create) {
                    ChannelFuture channelFuture = bootstrap.connect(RemotingUtil.addr2SocketAddress(addr));
                    cw = new ChannelWrapper(channelFuture);
                    channelTables.put(addr, cw);
                }
            }catch (Exception e) {
                logger.error("Create channel exception: " + e);
            }finally {
                lockChannelTables.unlock();
            }
        }else {
            logger.warn("Create channel timeout: " + LOCK_TIMEOUT);
        }

        if (cw != null) {
            ChannelFuture channelFuture = cw.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(clientConfig.getConnectTimeout())) {
                if (cw.isOK()) {
                    return cw.getChannel();
                }else {
                    logger.warn("Connect " + addr + " fail " + channelFuture.cause());
                }
            }else {
                logger.warn("Connect " + addr + " timeout.");
            }
        }

        return null;
    }

    static class ChannelWrapper {
        private final ChannelFuture channelFuture;

        public ChannelWrapper(final ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        public boolean isOK() {
            return channelFuture.channel().isActive();
        }

        public boolean isWritable() {
            return channelFuture.channel().isWritable();
        }

        private Channel getChannel() {
            return channelFuture.channel();
        }

        public ChannelFuture getChannelFuture() {
            return channelFuture;
        }
    }
}
