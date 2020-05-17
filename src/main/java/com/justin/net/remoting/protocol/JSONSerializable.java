package com.justin.net.remoting.protocol;

import com.alibaba.fastjson.JSON;

import java.nio.charset.Charset;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class JSONSerializable {
    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");

    public static byte[] encode(final Object obj) {
        String msg = toJson(obj, false);
        if (msg != null) {
            return msg.getBytes(CHARSET_UTF8);
        }
        return null;
    }

    public static  <T> T decode(byte[] data, Class<T> classOfT) {
        String msg = new String(data, CHARSET_UTF8);
        return fromJson(msg, classOfT);
    }

    private static String toJson(Object obj, boolean prettyFormat) {
        return JSON.toJSONString(obj, prettyFormat);
    }

    private static  <T> T fromJson(String msg, Class<T> classOfT) {
        return JSON.parseObject(msg, classOfT);
    }
}
