package com.justin.net.remoting.netty.event;

import io.netty.channel.Channel;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyEvent {
    private final NettyEventType type;
    private final String remoteAddr;
    private final Channel channel;

    public NettyEvent(final NettyEventType type, final String remoteAddr, final Channel channel) {
        this.type = type;
        this.remoteAddr = remoteAddr;
        this.channel = channel;
    }

    public NettyEventType getType() {
        return type;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public String toString() {
        return "NettyEvent [type=" + type + ", remoteAddr=" + remoteAddr + ", channel=" + channel + "]";
    }
}
