package com.justin.net.remoting.netty;

import com.justin.net.remoting.InvokeCallback;
import com.justin.net.remoting.ResponseProcessor;
import com.justin.net.remoting.common.SemaphoreReleaseOnlyOnce;
import io.netty.channel.Channel;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyResponseProcessor extends ResponseProcessor {
    private final Channel processChannel;

    public NettyResponseProcessor(Channel channel, int opaque, long timeout, InvokeCallback invokeCallback, SemaphoreReleaseOnlyOnce once) {
        super(opaque, timeout, invokeCallback, once);
        this.processChannel = channel;
    }

    public Channel getProcessChannel() {
        return processChannel;
    }
}
