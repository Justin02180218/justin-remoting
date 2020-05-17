package com.justin.net.remoting.netty.conf;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyClientConfig {
    private int workerThreads = 4;
    private int callbackExecutorThreads = Runtime.getRuntime().availableProcessors();
    private int onewayValue = 65535;
    private int asyncValue = 65535;
    private int connectTimeout = 3000;
    private int channelMaxIdleSeconds = 120;
    private long channelNotActiveInterval = 1000 * 60;

    private int socketSndBufSize = 65535;
    private int socketRcvBufSize = 65535;
    private boolean closeSocketByClient = false;

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getCallbackExecutorThreads() {
        return callbackExecutorThreads;
    }

    public void setCallbackExecutorThreads(int callbackExecutorThreads) {
        this.callbackExecutorThreads = callbackExecutorThreads;
    }

    public int getOnewayValue() {
        return onewayValue;
    }

    public void setOnewayValue(int onewayValue) {
        this.onewayValue = onewayValue;
    }

    public int getAsyncValue() {
        return asyncValue;
    }

    public void setAsyncValue(int asyncValue) {
        this.asyncValue = asyncValue;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getChannelMaxIdleSeconds() {
        return channelMaxIdleSeconds;
    }

    public void setChannelMaxIdleSeconds(int channelMaxIdleSeconds) {
        this.channelMaxIdleSeconds = channelMaxIdleSeconds;
    }

    public long getChannelNotActiveInterval() {
        return channelNotActiveInterval;
    }

    public void setChannelNotActiveInterval(long channelNotActiveInterval) {
        this.channelNotActiveInterval = channelNotActiveInterval;
    }

    public int getSocketSndBufSize() {
        return socketSndBufSize;
    }

    public void setSocketSndBufSize(int socketSndBufSize) {
        this.socketSndBufSize = socketSndBufSize;
    }

    public int getSocketRcvBufSize() {
        return socketRcvBufSize;
    }

    public void setSocketRcvBufSize(int socketRcvBufSize) {
        this.socketRcvBufSize = socketRcvBufSize;
    }

    public boolean isCloseSocketByClient() {
        return closeSocketByClient;
    }

    public void setCloseSocketByClient(boolean closeSocketByClient) {
        this.closeSocketByClient = closeSocketByClient;
    }
}
