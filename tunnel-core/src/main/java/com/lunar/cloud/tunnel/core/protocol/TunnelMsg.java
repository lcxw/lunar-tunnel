package com.lunar.cloud.tunnel.core.protocol;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TunnelMsg extends MessageBody {

    private MessageHeader header;
    private byte[] data;

    @Override
    public MessageHeader getMessageHeader() {
        return header;
    }

    @Override
    public byte[] getBody() {
        return data;
    }

    @Override

    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(MessageHeader.HEADER_LENGTH + data.length);
        buffer.putInt(header.getLength());
        buffer.put(header.encode());
        buffer.put(data);
        return buffer.array();
    }

    // 解码认证请求消息体
    @Override

    public TunnelMsg decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // 先解码header，然后解码body

        byte[] headerBytes = new byte[MessageHeader.HEADER_LENGTH];
        buffer.get(headerBytes);
        MessageHeader tempHeader = new MessageHeader();
        tempHeader = tempHeader.decode(headerBytes);
        byte[] bodyBytes = new byte[tempHeader.getLength() - MessageHeader.HEADER_LENGTH];
        buffer.get(bodyBytes);
        return new TunnelMsg(tempHeader, bodyBytes);
    }
}