package com.justin.net.remoting;

import com.justin.net.remoting.protocol.RemotingMessage;

import java.util.concurrent.ExecutorService;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public interface RemotingClient extends RemotingService {
    RemotingMessage invokeSync(final String addr, final RemotingMessage request, final long timeout) throws Exception;
    void invokeAsync(final String addr, final RemotingMessage request, final long timeout, final InvokeCallback invokeCallback) throws Exception;
    void invokeOneway(final String addr, final RemotingMessage request, final long timeout) throws Exception;

    void setCallbackExecutor(final ExecutorService executor);
    ExecutorService getCallbackExecutor();
}


