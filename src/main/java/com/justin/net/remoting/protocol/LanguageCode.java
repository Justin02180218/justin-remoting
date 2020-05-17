package com.justin.net.remoting.protocol;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public enum LanguageCode {
    JAVA((byte) 0),
    GOLANG((byte) 1);

    private byte code;

    LanguageCode(byte code) {
        this.code = code;
    }

    public static LanguageCode valueOf(byte code) {
        for (LanguageCode languageCode : LanguageCode.values()) {
            if (languageCode.getCode() == code) {
                return languageCode;
            }
        }
        return null;
    }

    public byte getCode() {
        return code;
    }
}
