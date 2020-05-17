package com.justin.net.remoting.netty.event;

import io.netty.channel.Channel;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public interface NettyEventListener {
    void onConnect(final String remoteAddr, final Channel channel);
    void onClose(final String remoteAddr, final Channel channel);
    void onException(final String remoteAddr, final Channel channel);
    void onIdle(final String remoteAddr, final Channel channel);
}
