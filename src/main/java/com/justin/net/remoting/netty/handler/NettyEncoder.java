package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.netty.event.NettyEvent;
import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
@ChannelHandler.Sharable
public class NettyEncoder extends MessageToByteEncoder<RemotingMessage> {
    private static final Logger logger = LogManager.getLogger(NettyEvent.class.getSimpleName());

    @Override
    protected void encode(ChannelHandlerContext ctx, RemotingMessage msg, ByteBuf byteBuf) throws Exception {
        try {
            ByteBuffer byteBuffer = msg.encode();
            byteBuf.writeBytes(byteBuffer);
        }catch(Exception e) {
            logger.error("encode exception " + e.getMessage());
            ctx.close();
        }
    }
}
