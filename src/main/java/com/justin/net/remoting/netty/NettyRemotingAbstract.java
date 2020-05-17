package com.justin.net.remoting.netty;

import com.justin.net.remoting.InvokeCallback;
import com.justin.net.remoting.RemotingService;
import com.justin.net.remoting.RequestProcessor;
import com.justin.net.remoting.common.Pair;
import com.justin.net.remoting.common.SemaphoreReleaseOnlyOnce;
import com.justin.net.remoting.netty.event.NettyEvent;
import com.justin.net.remoting.netty.event.NettyEventExecutor;
import com.justin.net.remoting.netty.event.NettyEventListener;
import com.justin.net.remoting.protocol.RemotingMessage;
import com.justin.net.remoting.protocol.RemotingMessageType;
import com.justin.net.remoting.protocol.SysResponseCode;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public abstract class NettyRemotingAbstract implements RemotingService {
    private static final Logger logger = LogManager.getLogger(NettyRemotingAbstract.class.getSimpleName());

    protected final Semaphore semaphoreOneway;
    protected final Semaphore semaphoreAsync;

    protected final ConcurrentHashMap<Integer, NettyResponseProcessor> responseTable = new ConcurrentHashMap<Integer, NettyResponseProcessor>(256);
    protected final HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>> processorTable = new HashMap<Integer, Pair<NettyRequestProcessor, ExecutorService>>(64);

    protected final NettyEventExecutor nettyEventExecutor = new NettyEventExecutor();

    protected Pair<NettyRequestProcessor, ExecutorService> defaultRequestProcessor;

    public NettyRemotingAbstract(final int permitsOneway, final int permitsAsync) {
        this.semaphoreOneway = new Semaphore(permitsOneway, true);
        this.semaphoreAsync = new Semaphore(permitsAsync, true);
    }

    public void processMessageReceived(ChannelHandlerContext ctx, RemotingMessage msg) {
        final RemotingMessage remotingMessage = msg;
        if (remotingMessage != null) {
            switch (msg.getMessageHeader().getMessageType()) {
                case REQUEST:
                case ONEWAY:
                    processRequestMessage(ctx, msg);
                    break;
                case RESPONSE:
                    processResponseMessage(ctx, msg);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void start() {
        NettyEventListener nettyEventListener = this.getNettyEventListener();
        if (nettyEventListener != null) {
            nettyEventExecutor.setEventListener(nettyEventListener);
            nettyEventExecutor.start();
        }
    }

    @Override
    public void shutdown() {
        if (!nettyEventExecutor.isStopped()) {
            nettyEventExecutor.shutdown();
        }
    }

    @Override
    public void registerProcessor(int requestCode, RequestProcessor processor, ExecutorService executor) {
        ExecutorService executorService = executor;
        if (executorService == null) {
            executorService = this.getPublicExecutor();
        }
        Pair<NettyRequestProcessor, ExecutorService> pair = new Pair<NettyRequestProcessor, ExecutorService>((NettyRequestProcessor) processor, executorService);
        processorTable.put(requestCode, pair);
    }

    public void putNettyEvent(final NettyEvent nettyEvent) {
        this.nettyEventExecutor.putNettyEvent(nettyEvent);
    }

    public void scanResponseTable() {
        final List<NettyResponseProcessor> list = new LinkedList<NettyResponseProcessor>();
        Iterator<Entry<Integer, NettyResponseProcessor>> it = responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, NettyResponseProcessor> next = it.next();
            NettyResponseProcessor responseProcessor = next.getValue();

            if (responseProcessor.getBeginTimestamp() + responseProcessor.getTimeout() + 1000 <= System.currentTimeMillis()) {
                responseProcessor.release();
                it.remove();
                list.add(responseProcessor);
                logger.info("remove timeout request");
            }
        }

        for (NettyResponseProcessor responseProcessor : list) {
            try {
                executeInvokeCallback(responseProcessor);
            }catch (Throwable e) {
                logger.error(e.getMessage());
            }
        }
    }

    public RemotingMessage invokeSyncImpl(final Channel channel, final RemotingMessage request, final long timeout) throws Exception {
        final int opaque = request.getMessageHeader().getOpaque();
        try {
            final NettyResponseProcessor responseProcessor = new NettyResponseProcessor(channel, opaque, timeout, null, null);
            responseTable.put(opaque, responseProcessor);
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        responseProcessor.setSendRequestOK(true);
                        return;
                    }else {
                        responseProcessor.setSendRequestOK(false);
                        responseProcessor.setCause(channelFuture.cause());
                        responseProcessor.putResponse(null);
                    }
                }
            });

            RemotingMessage response = responseProcessor.waitResponse(timeout);
            if (response == null) {
                if (responseProcessor.isSendRequestOK()) {
                    throw new Exception("Send request is success, but response is timeout!");
                }else {
                    throw new Exception("Send request is success, but response is fail!");
                }
            }

            return response;
        }finally {
            responseTable.remove(opaque);
        }
    }

    public void invokeAsyncImpl(final Channel channel, final RemotingMessage request, final long timeout, final InvokeCallback invokeCallback) throws Exception {
        final int opaque = request.getMessageHeader().getOpaque();
        long beginTime = System.currentTimeMillis();
        boolean acquire = semaphoreAsync.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        long costTime = System.currentTimeMillis() - beginTime;
        if (acquire) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(semaphoreAsync);
            if (costTime > timeout) {
                once.release();
                throw new Exception("invoke async call timeout!");
            }

            final NettyResponseProcessor responseProcessor = new NettyResponseProcessor(channel, opaque, timeout-costTime, invokeCallback, once);
            responseTable.put(opaque, responseProcessor);
            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            responseProcessor.setSendRequestOK(true);
                            return;
                        }
                        requestFail(opaque);
                    }
                });
            }catch (Exception e) {
                once.release();
                throw new Exception(e);
            }
        }else {
            if (timeout <= 0) {
                throw new Exception("invoke too fast!");
            }else {
                throw new Exception("SemaphoreAsync overload!");
            }
        }
    }

    public void invokeOnewayImpl(final Channel channel, final RemotingMessage request, final long timeout) throws Exception {
        request.getMessageHeader().setMessageType(RemotingMessageType.ONEWAY);
        boolean acquire = semaphoreOneway.tryAcquire(timeout, TimeUnit.MILLISECONDS);
        if (acquire) {
            final SemaphoreReleaseOnlyOnce once = new SemaphoreReleaseOnlyOnce(semaphoreOneway);
            try {
                channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        once.release();
                        if (!channelFuture.isSuccess()) {
                            logger.warn("send a request command to channel <" + channel.remoteAddress() + "> failed.");
                        }
                    }
                });
            }catch(Exception e) {
                once.release();
                throw new Exception(e);
            }
        }else {
            if (timeout <= 0) {
                throw new Exception("invoke too fast!");
            }else {
                throw new Exception("SemaphoreOneway overload!");
            }
        }
    }

    public void failFast(final Channel channel) {
        Iterator<Entry<Integer, NettyResponseProcessor>> it = responseTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, NettyResponseProcessor> entry = it.next();
            if (entry.getValue().getProcessChannel() == channel) {
                Integer opaque = entry.getKey();
                if (opaque != null) {
                    requestFail(opaque);
                }
            }
        }
    }

    public abstract NettyEventListener getNettyEventListener();
    public abstract ExecutorService getCallbackExecutor();
    public abstract ExecutorService getPublicExecutor();

    private void processRequestMessage(final ChannelHandlerContext ctx, final RemotingMessage request) {
        final Pair<NettyRequestProcessor, ExecutorService> processorByTable = processorTable.get(request.getMessageHeader().getCode());
        final Pair<NettyRequestProcessor, ExecutorService> processorExecutor = null == processorByTable ? defaultRequestProcessor: processorByTable;
        final int opaque = request.getMessageHeader().getOpaque();

        if (processorExecutor != null) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    try {
                        final RemotingMessage response = processorExecutor.getObject1().processRequest(ctx, request);

                        if (request.getMessageHeader().getMessageType() != RemotingMessageType.ONEWAY) {
                            response.getMessageHeader().setOpaque(opaque);
                            response.getMessageHeader().setMessageType(RemotingMessageType.RESPONSE);

                            try {
                                ctx.writeAndFlush(response);
                            }catch (Throwable e) {
                                logger.error("Response error " + e.getMessage());
                            }
                        }
                    }catch(Throwable e) {
                        logger.error("Process request error " + e.getMessage());
                        final RemotingMessage response = RemotingMessage.createResponseMessage(SysResponseCode.SYSTEM_ERROR.ordinal(), e.getMessage());
                        response.getMessageHeader().setOpaque(opaque);
                        ctx.writeAndFlush(response);
                    }
                }
            };

            if (processorExecutor.getObject1().rejectRequest()) {
                final RemotingMessage response = RemotingMessage.createResponseMessage(SysResponseCode.SYSTEM_BUSY.ordinal(), "[REJECTREQUEST] system busy");
                response.getMessageHeader().setOpaque(opaque);
                ctx.writeAndFlush(response);
            }

            try {
                processorExecutor.getObject2().submit(run);
            }catch (Exception e) {
                logger.error(e.getMessage());
                final RemotingMessage response = RemotingMessage.createResponseMessage(SysResponseCode.SYSTEM_BUSY.ordinal(), "[OVERLOAD] system busy");
                ctx.writeAndFlush(response);
            }
        }else {
            final RemotingMessage response = RemotingMessage.createResponseMessage(SysResponseCode.REQUEST_CODE_NOT_SUPPORTED.ordinal(), "request code not support");
            response.getMessageHeader().setOpaque(opaque);
            ctx.writeAndFlush(response);
        }
    }

    private void processResponseMessage(ChannelHandlerContext ctx, RemotingMessage msg) {
        final int opaque = msg.getMessageHeader().getOpaque();
        NettyResponseProcessor responseProcessor = responseTable.get(opaque);
        if (responseProcessor != null) {
            responseProcessor.setResponseMessage(msg);
            responseTable.remove(opaque);

            if (responseProcessor.getInvokeCallback() != null) {
                executeInvokeCallback(responseProcessor);
            }else {
                responseProcessor.putResponse(msg);
                responseProcessor.release();
            }
        }else {
            logger.warn("receive response, but not matched any request by " + ctx.channel().remoteAddress().toString());
        }
    }

    private void executeInvokeCallback(final NettyResponseProcessor responseProcessor) {
        boolean flag = false;
        ExecutorService executor = this.getCallbackExecutor();
        if (executor != null) {
            try {
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            responseProcessor.executeInvokeCallback();
                        }catch (Throwable e) {
                            logger.error(e.getMessage());
                        }finally {
                            responseProcessor.release();
                        }
                    }
                });
            }catch(Exception e) {
                logger.error(e.getMessage());
                flag = true;
            }
        }else {
            flag = true;
        }

        if (flag) {
            try {
                responseProcessor.executeInvokeCallback();
            }catch (Throwable e) {
                logger.error(e.getMessage());
            }finally {
                responseProcessor.release();
            }
        }
    }

    private void requestFail(final int opaque) {
        NettyResponseProcessor responseProcessor = responseTable.get(opaque);
        if (responseProcessor != null) {
            responseProcessor.setSendRequestOK(false);
            responseProcessor.putResponse(null);
            try {
                executeInvokeCallback(responseProcessor);
            }catch (Throwable e) {
                logger.error("Request fail " + e.getMessage());
            }finally {
                responseProcessor.release();
            }
        }
    }
 }
