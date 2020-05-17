package com.justin.net.remoting;

import com.justin.net.remoting.common.SemaphoreReleaseOnlyOnce;
import com.justin.net.remoting.protocol.RemotingMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class ResponseProcessor {
    private final int opaque;
    private final long timeout;
    private final InvokeCallback invokeCallback;
    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final SemaphoreReleaseOnlyOnce once;
    private final AtomicBoolean executeCallbackOnlyOnce = new AtomicBoolean(false);

    private volatile RemotingMessage responseMessage;
    private volatile boolean sendRequestOK = true;
    private volatile Throwable cause;

    public ResponseProcessor(int opaque, long timeout, InvokeCallback invokeCallback, SemaphoreReleaseOnlyOnce once) {
        this.opaque = opaque;
        this.timeout = timeout;
        this.invokeCallback = invokeCallback;
        this.once = once;
    }

    public void executeInvokeCallback() {
        if (invokeCallback != null) {
            if (executeCallbackOnlyOnce.compareAndSet(false, true)) {
                invokeCallback.operationComplete(this);
            }
        }
    }

    public void release() {
        if (once != null) {
            once.release();
        }
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - beginTimestamp;
        return diff > timeout;
    }

    public RemotingMessage waitResponse(final long timeout) throws InterruptedException {
        countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
        return responseMessage;
    }

    public void putResponse(RemotingMessage responseMessage) {
        this.responseMessage = responseMessage;
        countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeout() {
        return timeout;
    }

    public InvokeCallback getInvokeCallback() {
        return invokeCallback;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public RemotingMessage getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(RemotingMessage responseMessage) {
        this.responseMessage = responseMessage;
    }

    public int getOpaque() {
        return opaque;
    }

    @Override
    public String toString() {
        return "ResponseProcessor [responseMessage=" + responseMessage
                + ", sendRequestOK=" + sendRequestOK
                + ", cause=" + cause
                + ", opaque=" + opaque
                + ", timeout=" + timeout
                + ", invokeCallback=" + invokeCallback
                + ", beginTimestamp=" + beginTimestamp
                + ", countDownLatch=" + countDownLatch + "]";
    }
}
