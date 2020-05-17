package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.common.RemotingUtil;
import com.justin.net.remoting.netty.NettyRemotingClient;
import com.justin.net.remoting.netty.event.NettyEvent;
import com.justin.net.remoting.netty.event.NettyEventType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyClientConnMgrHandler extends ChannelDuplexHandler {
    private static final Logger logger = LogManager.getLogger(NettyClientConnMgrHandler.class.getSimpleName());

    private final NettyRemotingClient remotingClient;

    public NettyClientConnMgrHandler(final NettyRemotingClient remotingClient) {
        this.remotingClient = remotingClient;
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(remoteAddress);
        final String localAddr = RemotingUtil.parseSocketAddressAddr(localAddress);
        logger.info("Client connect: local[" + localAddr + "], remote[" + remoteAddr + "]");

        super.connect(ctx, remoteAddress, localAddress, promise);

        if (remotingClient.getNettyEventListener() != null) {
            remotingClient.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddr, ctx.channel()));
        }
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Client disconnect: remote[" + remoteAddr + "]");

        remotingClient.closeChannel(ctx.channel());
        super.disconnect(ctx, promise);

        if (remotingClient.getNettyEventListener() != null) {
            remotingClient.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddr, ctx.channel()));
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Client close: remote[" + remoteAddr + "]");

        remotingClient.closeChannel(ctx.channel());
        super.close(ctx, promise);

        remotingClient.failFast(ctx.channel());
        if (remotingClient.getNettyEventListener() != null) {
            remotingClient.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddr, ctx.channel()));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
                logger.info("Client idle: remote[" + remoteAddr + "]");

                remotingClient.closeChannel(ctx.channel());
                if (remotingClient.getNettyEventListener() != null) {
                    remotingClient.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddr, ctx.channel()));
                }
            }
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Client exception: remote[" + remoteAddr + "], cause[" + cause + "]");

        remotingClient.closeChannel(ctx.channel());
        if (remotingClient.getNettyEventListener() != null) {
            remotingClient.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddr, ctx.channel()));
        }
    }
}
