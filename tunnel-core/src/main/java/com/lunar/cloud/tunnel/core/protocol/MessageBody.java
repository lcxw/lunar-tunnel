package com.lunar.cloud.tunnel.core.protocol;

public abstract class MessageBody {
    public abstract MessageHeader getMessageHeader();

    public abstract byte[] getBody();

    public abstract byte[] encode();

    public abstract MessageBody decode(byte[] data);
}
