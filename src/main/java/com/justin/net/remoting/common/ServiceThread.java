package com.justin.net.remoting.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public abstract class ServiceThread implements Runnable {
    private static final Logger logger = LogManager.getLogger(ServiceThread.class.getSimpleName());

    private static final long JOIN_TIME = 90 * 1000;
    private final Thread thread;
    protected volatile boolean hasNotified = false;
    protected volatile boolean stopped = false;

    public ServiceThread() {
        thread = new Thread(this, getServiceName());
    }

    public void start() {
        thread.start();
    }

    public void shutdown() {
        this.shutdown(false);
    }

    public void shutdown(final boolean interrupt) {
        stopped = true;
        logger.info("shutdown thread: " + getServiceName() + " interrupt: " + interrupt);
        synchronized (this) {
            if (!hasNotified) {
                hasNotified = true;
                this.notify();
            }
        }

        try {
            if (interrupt) {
                thread.interrupt();
            }

            long beginTime = System.currentTimeMillis();
            thread.join(this.getJoinTime());
            long elapsedTime = System.currentTimeMillis() - beginTime;
            logger.info("join thread " + this.getServiceName() + " elapsed time(ms) " + elapsedTime + " " + this.getJoinTime());
        }catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public abstract String getServiceName();

    public static long getJoinTime() {
        return JOIN_TIME;
    }

    public boolean isStopped() {
        return stopped;
    }
}
