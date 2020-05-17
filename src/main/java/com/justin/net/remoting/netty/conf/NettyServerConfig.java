package com.justin.net.remoting.netty.conf;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyServerConfig {
    private String listenAddr = "127.0.0.1";
    private int listenPort = 9999;
    private int selectorThreads = 3;
    private int workerThreads = 5;
    private int callbackExecutorThreads = 4;
    private int asyncValue = 64;
    private int onewayValue = 256;
    private int channelMaxIdleSeconds = 120;

    private int socketSndBufSize = 65535;
    private int socketRcvBufSize = 65535;

    public NettyServerConfig(){}

    public NettyServerConfig(String listenAddr, int listenPort) {
        this.listenAddr = listenAddr;
        this.listenPort = listenPort;
    }

    public String getListenAddr() {
        return listenAddr;
    }

    public void setListenAddr(String listenAddr) {
        this.listenAddr = listenAddr;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public int getSelectorThreads() {
        return selectorThreads;
    }

    public void setSelectorThreads(int selectorThreads) {
        this.selectorThreads = selectorThreads;
    }

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

    public int getAsyncValue() {
        return asyncValue;
    }

    public void setAsyncValue(int asyncValue) {
        this.asyncValue = asyncValue;
    }

    public int getOnewayValue() {
        return onewayValue;
    }

    public void setOnewayValue(int onewayValue) {
        this.onewayValue = onewayValue;
    }

    public int getChannelMaxIdleSeconds() {
        return channelMaxIdleSeconds;
    }

    public void setChannelMaxIdleSeconds(int channelMaxIdleSeconds) {
        this.channelMaxIdleSeconds = channelMaxIdleSeconds;
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
}
