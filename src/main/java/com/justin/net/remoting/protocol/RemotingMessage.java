package com.justin.net.remoting.protocol;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class RemotingMessage {
    private static final Logger logger = LogManager.getLogger(RemotingMessage.class.getSimpleName());
    private static SerializeType serializeType = SerializeType.JSON;

    private transient RemotingMessageHeader messageHeader;
    private transient byte[] messageBody;

    public RemotingMessage(){}

    public RemotingMessage(final RemotingMessageHeader messageHeader, final byte[] messageBody) {
        this.messageHeader = messageHeader;
        this.messageBody = messageBody;
    }

    public ByteBuffer encode() {
        // 消息字节长度
        int length = 4;
        // 序列化后的消息头数据
        byte[] headerData = this.serializeHeaderData();
        // 加上消息头的字节长度
        length += headerData.length;
        // 加上消息体的长度
        if (messageBody != null) {
            length += messageBody.length;
        }

        // 按通信协议转换成字节序列
        // |<- length ->|<- header_length ->|<- header_data ->|<- body data ->|
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + length);
        byteBuffer.putInt(length);
        byteBuffer.put(markSerializeType(headerData.length, serializeType));
        byteBuffer.put(headerData);
        if (messageBody != null) {
            byteBuffer.put(messageBody);
        }
        byteBuffer.flip();

        return byteBuffer;
    }

    public static RemotingMessage decode(final ByteBuffer byteBuffer) {
        // 消息总字节长度
        int length = byteBuffer.getInt();
        // 第一个字节表示序列化类型，后三个字节表示消息头长度
        int headerLength = byteBuffer.getInt();
        // 消息头字节长度，headerLength的后三位字节表示的长度
        int realHeaderLength = getRealHeaderLength(headerLength);

        byte[] headerData = new byte[realHeaderLength];
        byteBuffer.get(headerData);
        // 反序列化成消息头对象
        RemotingMessageHeader messageHeader = unserializeHeaderData(headerData, getSerializeType(headerLength));

        int bodyLength = length - 4 - headerLength;
        byte[] bodyData = null;
        if (bodyLength > 0) {
            bodyData = new byte[bodyLength];
            byteBuffer.get(bodyData);
        }

        return new RemotingMessage(messageHeader, bodyData);
    }

    public static RemotingMessage createResponseMessage(int code, String remark) {
        RemotingMessageHeader header = new RemotingMessageHeader();
        header.setCode(code);
        header.setRemark(remark);
        header.setMessageType(RemotingMessageType.RESPONSE);
        return new RemotingMessage(header, null);
    }

    private byte[] serializeHeaderData() {
        if (serializeType == SerializeType.JSON) {
            return JSONSerializable.encode(messageHeader);
        }
        return null;
    }

    private static RemotingMessageHeader unserializeHeaderData(byte[] headerData, SerializeType type) {
        switch (type) {
            case JSON:
                return JSONSerializable.decode(headerData, RemotingMessageHeader.class);
        }
        return null;
    }

    private byte[] markSerializeType(int source, SerializeType type) {
        byte[] result = new byte[4];
        result[0] = type.getCode();
        result[1] = (byte) ((source >> 16) & 0xFF);
        result[2] = (byte) ((source >> 8) & 0xFF);
        result[3] = (byte) (source & 0xFF);

        return result;
    }

    private static int getRealHeaderLength(int headerLength) {
        return headerLength & 0xFFFFFF;
    }

    private static SerializeType getSerializeType(int headerLength) {
        SerializeType type = SerializeType.valueOf((byte) ((headerLength >> 24) & 0xFF));
        serializeType = type;
        return type;
    }

    public static SerializeType getSerializeType() {
        return serializeType;
    }

    public final RemotingMessageHeader getMessageHeader() {
        return messageHeader;
    }
    public final void setMessageHeader(RemotingMessageHeader messageHeader) {
        this.messageHeader = messageHeader;
    }
    public final byte[] getMessageBody() {
        return messageBody;
    }
    public final void setMessageBody(byte[] messageBody) {
        this.messageBody = messageBody;
    }
}
