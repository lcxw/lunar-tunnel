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
        byte msgType = msg.getType();
        out.writeByte(msgType);
        switch (msgType){
            case TunnelMsg.TYPE_AUTH:
                out.writeByte(msg.getData()[0]);
                break;
            case TunnelMsg.TYPE_TRANSFER:
                if (msg.getData() != null) {
                    out.writeBytes(msg.getData());
                }
                break;
            default:
                break;
        }

    }
}
