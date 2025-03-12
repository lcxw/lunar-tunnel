package com.lunar.cloud.tunnel.core.protocol;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class TunnelMsgEncoder extends MessageToByteEncoder<MessageBody> {


    @Override
    protected void encode(ChannelHandlerContext ctx, MessageBody msg, ByteBuf out) {
        MessageHeader messageHeader = msg.getMessageHeader();
        int length = messageHeader.getLength();
        out.writeInt(length);
        out.writeBytes(messageHeader.encode());
        out.writeInt(length);
        out.writeBytes(msg.encode());
    }
}
