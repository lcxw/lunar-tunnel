package com.lunar.cloud.tunnel.core.protocol;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TunnelMsgDecoder extends LengthFieldBasedFrameDecoder {

    public TunnelMsgDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
                        int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public TunnelMsgDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
                        int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    @Override
    protected TunnelMsg decode(ChannelHandlerContext ctx, ByteBuf in2) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, in2);
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < 4) {
            return null;
        }

        TunnelMsg tunnelMsg = new TunnelMsg();
        int dataLength = in.readInt();
        byte type = in.readByte();
        tunnelMsg.setType(type);
        byte[] data = new byte[dataLength - 5];
        in.readBytes(data);
        tunnelMsg.setData(data);
        in.release();

        return tunnelMsg;
    }
}