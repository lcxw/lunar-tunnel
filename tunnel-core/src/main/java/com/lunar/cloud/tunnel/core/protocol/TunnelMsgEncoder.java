package com.lunar.cloud.tunnel.core.protocol;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
public class TunnelMsgEncoder extends MessageToByteEncoder<TunnelMsg> {


    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelMsg msg, ByteBuf out) {
        int bodyLength = 5;
        if (msg.getData() != null) {
            bodyLength += msg.getData().length;
        }

        out.writeInt(bodyLength);

        out.writeByte(msg.getType());

        if (msg.getData() != null) {
            out.writeBytes(msg.getData());
        }
    }
}
