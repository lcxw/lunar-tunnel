package com.lunar.cloud.tunnel.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHeader {
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    public static final byte VERSION = 1;
    public static final int HEADER_LENGTH = 12;
    private MessageType type;
    private short sequenceId;
    private int length;

    /**
     * 编码消息头
     *
     * @return byte[]
     */
    public byte[] encode() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 1 + 2 + 4);
        buffer.put((byte) type.getValue());
        buffer.putShort(sequenceId);
        buffer.putInt(length);
        return buffer.array();
    }

    // 解码消息头
    public MessageHeader decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic = buffer.getInt();
        if (magic != MAGIC_NUMBER) {
            throw new IllegalArgumentException("Unknown magic number: " + magic);
        }
        byte version = buffer.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unknown version: " + version);
        }
        int typeValue = buffer.get();
        MessageType t = MessageType.values()[typeValue];
        if (t == null) {
            throw new IllegalArgumentException("Unknown message type: " + typeValue);
        }
        short sequenceNo = buffer.getShort();

        int len = buffer.getInt();
        return new MessageHeader(t,sequenceNo, len);
    }
}
