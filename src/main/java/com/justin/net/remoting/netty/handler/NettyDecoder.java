package com.justin.net.remoting.netty.handler;

import com.justin.net.remoting.protocol.RemotingMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class NettyDecoder extends LengthFieldBasedFrameDecoder {
    private static final Logger logger = LogManager.getLogger(NettyDecoder.class.getSimpleName());

    private static final int MAX_FRAME_LENGTH = Integer.MAX_VALUE;

    public NettyDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (frame == null) {
                return null;
            }
            ByteBuffer byteBuffer = frame.nioBuffer();
            return RemotingMessage.decode(byteBuffer);
        }catch (Exception e) {
            logger.error("Decode exception " + ctx.channel().remoteAddress().toString() + e);
            ctx.channel().close();
        }finally {
            if (frame != null) {
                frame.release();
            }
        }
        return null;
    }
}
