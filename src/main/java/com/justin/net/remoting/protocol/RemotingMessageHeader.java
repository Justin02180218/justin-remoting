package com.justin.net.remoting.protocol;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WX: coding到灯火阑珊
 * @author Justin
 */
public class RemotingMessageHeader {
    private static final AtomicInteger requestId = new AtomicInteger(0);

    private int code;
    private LanguageCode language = LanguageCode.JAVA;
    private int version = 0;
    private int opaque = requestId.getAndIncrement();
    private RemotingMessageType messageType = RemotingMessageType.REQUEST;
    private String remark;
    private HashMap<String, String> extFields = new HashMap<String, String>();

    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public LanguageCode getLanguage() {
        return language;
    }
    public void setLanguage(LanguageCode language) {
        this.language = language;
    }
    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public RemotingMessageType getMessageType() {
        return messageType;
    }
    public void setMessageType(RemotingMessageType messageType) {
        this.messageType = messageType;
    }
    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }
    public HashMap<String, String> getExtFields() {
        return extFields;
    }
    public void setExtFields(HashMap<String, String> extFields) {
        this.extFields = extFields;
    }
    public int getOpaque() {
        return opaque;
    }
    public void setOpaque(int opaque) {
        this.opaque = opaque;
    }
}
