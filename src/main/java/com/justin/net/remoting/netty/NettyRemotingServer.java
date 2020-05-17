package com.justin.net.remoting.netty;

import com.justin.net.remoting.InvokeCallback;
import com.justin.net.remoting.RemotingServer;
import com.justin.net.remoting.RequestProcessor;
import com.justin.net.remoting.common.Pair;
import com.justin.net.remoting.netty.conf.NettyServerConfig;
import com.justin.net.remoting.netty.event.NettyEventListener;
import com.justin.net.remoting.netty.handler.NettyServerConnMgrHandler;
import com.justin.net.remoting.netty.handler.NettyDecoder;
import com.justin.net.remoting.netty.handler.NettyEncoder;
import com.justin.net.remoting.netty.handler.NettyServerHandler;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyRemotingServer extends NettyRemotingAbstract implements RemotingServer {
    private static final Logger logger = LogManager.getLogger(NettyRemotingServer.class.getSimpleName());

    private final ServerBootstrap serverBootstrap;
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup selectors;
    private final NettyServerConfig serverConfig;

    private final ExecutorService publicExecutor;
    private final NettyEventListener eventListener;
    private final Timer timer = new Timer("ServerHouseKeepingService", true);

    private DefaultEventExecutorGroup defaultEventExecutorGroup;
    private int port = 0;

    public NettyRemotingServer(final NettyServerConfig serverConfig) {
        this(serverConfig, null);
    }

    public NettyRemotingServer(final NettyServerConfig serverConfig, final NettyEventListener eventListener) {
        super(serverConfig.getOnewayValue(), serverConfig.getAsyncValue());
        this.serverBootstrap = new ServerBootstrap();
        this.serverConfig = serverConfig;
        this.eventListener = eventListener;

        this.publicExecutor = Executors.newFixedThreadPool(serverConfig.getCallbackExecutorThreads(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_public_executor_" + index.getAndIncrement());
            }
        });

        this.boss = new NioEventLoopGroup(1, new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_boss_" + index.getAndIncrement());
            }
        });

        this.selectors = new NioEventLoopGroup(serverConfig.getSelectorThreads(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_selector_" + index.getAndIncrement());
            }
        });
    }

    @Override
    public void start() {
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(serverConfig.getWorkerThreads(), new ThreadFactory() {
            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Server_worker_" + index.getAndIncrement());
            }
        });

        serverBootstrap.group(boss, selectors)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_SNDBUF, serverConfig.getSocketSndBufSize())
                .childOption(ChannelOption.SO_RCVBUF, serverConfig.getSocketRcvBufSize())
                .localAddress(new InetSocketAddress(serverConfig.getListenAddr(), serverConfig.getListenPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(defaultEventExecutorGroup,
                                new NettyEncoder(),
                                new NettyDecoder(),
                                new IdleStateHandler(0, 0, serverConfig.getChannelMaxIdleSeconds()),
                                new NettyServerConnMgrHandler(NettyRemotingServer.this),
                                new NettyServerHandler(NettyRemotingServer.this)
                        );
                    }
                });

        try {
            ChannelFuture sync = serverBootstrap.bind().sync();
            InetSocketAddress addr = (InetSocketAddress) sync.channel().localAddress();
            this.port = addr.getPort();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        super.start();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    NettyRemotingServer.this.scanResponseTable();
                }catch (Throwable e) {
                    logger.info("Server Scan response table " + e.getMessage());
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

            boss.shutdownGracefully();
            selectors.shutdownGracefully();

            super.shutdown();

            if (defaultEventExecutorGroup != null) {
                defaultEventExecutorGroup.shutdownGracefully();
            }

            if (publicExecutor != null) {
                publicExecutor.shutdown();
            }
        }catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public NettyEventListener getNettyEventListener() {
        return eventListener;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return publicExecutor;
    }

    @Override
    public ExecutorService getPublicExecutor() {
        return publicExecutor;
    }


    @Override
    public void registerDefaultProcessor(RequestProcessor processor, ExecutorService executor) {
        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>((NettyRequestProcessor)processor, executor);
        this.defaultRequestProcessor = pair;
    }

    public void invokeSync(final Channel channel, final RemotingMessage request, final long timeout) throws Exception {
        this.invokeSyncImpl(channel, request, timeout);
    }

    public void invokeAsync(final Channel channel, final RemotingMessage request, final long timeout, final InvokeCallback invokeCallback) throws Exception {
        this.invokeAsyncImpl(channel, request, timeout, invokeCallback);
    }

    public void invokeOneway(final Channel channel, final RemotingMessage request, final long timeout) throws Exception {
        this.invokeOnewayImpl(channel, request, timeout);
    }
}
