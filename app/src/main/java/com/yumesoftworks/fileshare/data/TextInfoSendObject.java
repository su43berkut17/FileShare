package com.yumesoftworks.fileshare.data;

import java.io.Serializable;

public class TextInfoSendObject implements Serializable {
    private static final long serialversionUID = 129348938L;
    private int messageType;
    private String messageContent;
    private String additionalInfo;

    public TextInfoSendObject(int messageType, String messageContent, String additionalInfo){
        this.messageType=messageType;
        this.messageContent=messageContent;
        this.additionalInfo=additionalInfo;
    }

    public int getMessageType() {
        return messageType;
    }
    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }
    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}
