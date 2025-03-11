package com.lunar.cloud.tunnel.core.protocol;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor()
public class TunnelAuthMsg extends MessageBody {

    private MessageHeader header;
    private String appId;
    private String appSecret;

    @Override
    public MessageHeader getMessageHeader() {
        return header;
    }

    @Override
    public byte[] getBody() {
        return (appId+appSecret).getBytes();
    }

    // 编码认
    // 证请求消息体
    @Override

    public byte[] encode() {
        byte[] appidBytes = appId.getBytes();
        byte[] passwordBytes = appSecret.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(MessageHeader.HEADER_LENGTH +4 + appidBytes.length + 4 + passwordBytes.length);
        buffer.putInt(header.getLength());
        buffer.put(header.encode());
        buffer.putInt(appidBytes.length);
        buffer.put(appidBytes);
        buffer.putInt(passwordBytes.length);
        buffer.put(passwordBytes);
        return buffer.array();
    }
    // 解码认证请求消息体
    @Override

    public  TunnelAuthMsg decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] headerBytes = new byte[MessageHeader.HEADER_LENGTH];
        buffer.get(headerBytes);
        MessageHeader tempHeader = new MessageHeader();
        tempHeader = tempHeader.decode(headerBytes);
        int appidLength = buffer.getInt();
        byte[] appidBytes = new byte[appidLength];
        buffer.get(appidBytes);
        int passwordLength = buffer.getInt();
        byte[] passwordBytes = new byte[passwordLength];
        buffer.get(passwordBytes);
        String appid = new String(appidBytes);
        String password = new String(passwordBytes);
        return new TunnelAuthMsg(tempHeader,appid, password);
    }
}