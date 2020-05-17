package com.justin.net.remoting;

import com.justin.net.remoting.netty.NettyRemotingClient;
import com.justin.net.remoting.netty.NettyRemotingServer;
import com.justin.net.remoting.netty.NettyRequestProcessor;
import com.justin.net.remoting.netty.conf.NettyClientConfig;
import com.justin.net.remoting.netty.conf.NettyServerConfig;
import com.justin.net.remoting.protocol.RemotingMessage;
import com.justin.net.remoting.protocol.RemotingMessageHeader;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.concurrent.Executors;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class RemotingTest {
    private NettyRemotingServer remotingServer;
    private NettyRemotingClient remotingClient;

    @Before
    public void setup() {
        remotingServer = new NettyRemotingServer(new NettyServerConfig());
        remotingServer.registerProcessor(0, new NettyRequestProcessor() {
            @Override
            public RemotingMessage processRequest(ChannelHandlerContext ctx, RemotingMessage request) throws Exception {
                request.getMessageHeader().setRemark("Hello, " + ctx.channel().remoteAddress() + " I'm server");
                request.setMessageBody("I'm here!".getBytes(Charset.forName("UTF-8")));
                return request;
            }

            @Override
            public boolean rejectRequest() {
                return false;
            }
        }, Executors.newCachedThreadPool());
        remotingServer.start();

        remotingClient = new NettyRemotingClient(new NettyClientConfig());
        remotingClient.start();
    }

    @After
    public void destroy() {
        try {
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        remotingServer.shutdown();
        remotingClient.shutdown();
    }

    @Test
    public void testInvokeOneway() throws Exception {
        RemotingMessageHeader header = new RemotingMessageHeader();
        header.setCode(0);
        header.setRemark("I'm a oneway test1");
        RemotingMessage request = new RemotingMessage(header, null);

        remotingClient.invokeOneway("127.0.0.1:9999", request, 3*1000);
    }

    @Test
    public void testInvokeSync() throws Exception {
        RemotingMessageHeader header = new RemotingMessageHeader();
        header.setCode(0);
        header.setRemark("I'm a sync test!");
        byte[] body = "Waiting for you!".getBytes(Charset.forName("UTF-8"));
        RemotingMessage request = new RemotingMessage(header, body);

        RemotingMessage response = remotingClient.invokeSync("127.0.0.1:9999", request, 3*1000);
        System.out.println("1 --> " + response.getMessageHeader().getCode());
        System.out.println("2 --> " + response.getMessageHeader().getOpaque());
        System.out.println("3 --> " + response.getMessageHeader().getRemark());
        System.out.println("4 --> " + new String(response.getMessageBody(), Charset.forName("UTF-8")));
    }

    @Test
    public void testInvokeAsync() throws Exception {
        RemotingMessageHeader header = new RemotingMessageHeader();
        header.setCode(0);
        header.setRemark("I'm a async test!");
        byte[] body = "I will come back!".getBytes(Charset.forName("UTF-8"));
        final RemotingMessage request = new RemotingMessage(header, body);

        remotingClient.invokeAsync("127.0.0.1:9999", request, 3 * 1000, new InvokeCallback() {
            @Override
            public void operationComplete(ResponseProcessor responseProcessor) {
                RemotingMessage response = responseProcessor.getResponseMessage();
                System.out.println(response.getMessageHeader().getCode());
                System.out.println(response.getMessageHeader().getOpaque());
                System.out.println(response.getMessageHeader().getRemark());
                System.out.println(new String(response.getMessageBody(), Charset.forName("UTF-8")));
            }
        });
    }
}