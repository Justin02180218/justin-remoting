package com.justin.net.remoting.protocol;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public enum SerializeType {
    JSON((byte) 0),
    CUSTOM((byte) 1);

    private byte code;

    SerializeType(byte code) {
        this.code = code;
    }

    public static SerializeType valueOf(byte code) {
        for(SerializeType serializeType : SerializeType.values()) {
            if (serializeType.getCode() == code) {
                return serializeType;
            }
        }
        return null;
    }

    public byte getCode() {
        return code;
    }
}
