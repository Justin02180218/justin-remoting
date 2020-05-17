package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.netty.NettyRemotingClient;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<RemotingMessage> {
    private final NettyRemotingClient remotingClient;

    public NettyClientHandler(final NettyRemotingClient remotingClient) {
        this.remotingClient = remotingClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemotingMessage msg) throws Exception {
        remotingClient.processMessageReceived(ctx, msg);
    }
}
