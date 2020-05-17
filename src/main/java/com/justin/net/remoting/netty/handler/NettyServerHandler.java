package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.netty.NettyRemotingServer;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<RemotingMessage> {
    private final NettyRemotingServer nettyRemoting;

    public NettyServerHandler(final NettyRemotingServer nettyRemoting) {
        this.nettyRemoting = nettyRemoting;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingMessage msg) throws Exception {
        nettyRemoting.processMessageReceived(ctx, msg);
    }
}
