package com.justin.net.remoting;

import java.util.concurrent.ExecutorService;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public interface RemotingService {
    void start();
    void shutdown();
    void registerProcessor(final int requestCode, final RequestProcessor processor, final ExecutorService executor);

}
