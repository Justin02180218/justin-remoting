package com.justin.net.remoting.netty.event;

import com.justin.net.remoting.common.ServiceThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyEventExecutor extends ServiceThread {
    private static final Logger logger = LogManager.getLogger(NettyEventExecutor.class.getSimpleName());

    private final LinkedBlockingQueue<NettyEvent> queue = new LinkedBlockingQueue<NettyEvent>();
    private final int maxSize = 10000;

    private NettyEventListener eventListener;

    public void putNettyEvent(final NettyEvent event) {
        if (queue.size() < maxSize) {
            queue.add(event);
        }
    }

    public void run() {
        while (!this.isStopped()) {
            NettyEvent nettyEvent = null;
            try {
                nettyEvent = queue.poll(3000, TimeUnit.MILLISECONDS);
                if (nettyEvent!=null && eventListener!=null) {
                    switch (nettyEvent.getType()) {
                        case CONNECT:
                            eventListener.onConnect(nettyEvent.getRemoteAddr(), nettyEvent.getChannel());
                            break;
                        case CLOSE:
                            eventListener.onClose(nettyEvent.getRemoteAddr(), nettyEvent.getChannel());
                            break;
                        case IDLE:
                            eventListener.onIdle(nettyEvent.getRemoteAddr(), nettyEvent.getChannel());
                            break;
                        case EXCEPTION:
                            eventListener.onException(nettyEvent.getRemoteAddr(), nettyEvent.getChannel());
                            break;
                        default:
                            break;
                    }
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public NettyEventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(NettyEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public String getServiceName() {
        return NettyEventExecutor.class.getSimpleName();
    }
}
