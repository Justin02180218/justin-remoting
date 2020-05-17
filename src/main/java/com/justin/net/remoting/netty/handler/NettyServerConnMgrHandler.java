package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.common.RemotingUtil;
import com.justin.net.remoting.netty.NettyRemotingServer;
import com.justin.net.remoting.netty.event.NettyEvent;
import com.justin.net.remoting.netty.event.NettyEventType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
@ChannelHandler.Sharable
public class NettyServerConnMgrHandler extends ChannelDuplexHandler {
    private static final Logger logger = LogManager.getLogger(NettyServerConnMgrHandler.class.getSimpleName());

    private final NettyRemotingServer remotingServer;

    public NettyServerConnMgrHandler(final NettyRemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Server Channel Registered remote[" + remoteAddr + "]");
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Server Channel Unregistered remote[" + remoteAddr + "]");
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Server Channel connect remote[" + remoteAddr + "]");
        super.channelActive(ctx);

        if (remotingServer.getNettyEventListener() != null ) {
            remotingServer.putNettyEvent(new NettyEvent(NettyEventType.CONNECT, remoteAddr, ctx.channel()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Server Channel close remote[" + remoteAddr + "]");
        super.channelInactive(ctx);

        if (remotingServer.getNettyEventListener() != null) {
            remotingServer.putNettyEvent(new NettyEvent(NettyEventType.CLOSE, remoteAddr, ctx.channel()));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if (event instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) event;
            if (idleStateEvent.state().equals(IdleState.ALL_IDLE)) {
                final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
                logger.info("Server Idle remote[" + remoteAddr + "]");
                ctx.channel().close();

                if (remotingServer.getNettyEventListener() != null) {
                    remotingServer.putNettyEvent(new NettyEvent(NettyEventType.IDLE, remoteAddr, ctx.channel()));
                }
            }
        }
        ctx.fireUserEventTriggered(event);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        final String remoteAddr = RemotingUtil.parseSocketAddressAddr(ctx.channel().remoteAddress());
        logger.info("Server Exception remote[" + remoteAddr + "], cause[" + cause + "]");

        if (remotingServer.getNettyEventListener() != null) {
            remotingServer.putNettyEvent(new NettyEvent(NettyEventType.EXCEPTION, remoteAddr, ctx.channel()));
        }

        ctx.channel().close();
    }
}
