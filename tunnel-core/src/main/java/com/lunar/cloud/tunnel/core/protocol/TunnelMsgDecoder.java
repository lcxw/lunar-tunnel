package com.lunar.cloud.tunnel.core.protocol;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 隧道消息解码器，基于Netty长度字段帧解码器实现
 * <p>负责将字节流解析为TunnelMsg对象，遵循以下协议格式：
 * [长度字段(4字节)][类型(1字节)][数据载荷(N字节)]</p>
 */
@Slf4j
public class TunnelMsgDecoder extends LengthFieldBasedFrameDecoder {
    /**
     * 解码器构造函数
     *
     * @param maxFrameLength      最大帧长度（包含长度字段自身）
     * @param lengthFieldOffset   长度字段偏移量（从帧头开始计算）
     * @param lengthFieldLength   长度字段自身字节长度（通常为4字节）
     * @param lengthAdjustment    长度字段值调整量（实际载荷长度 = 长度字段值 + 此调整量）
     * @param initialBytesToStrip 需要跳过的初始字节数（通常是长度字段的字节长度）
     */
    public TunnelMsgDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
                            int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    /**
     * 解码器构造函数（带快速失败模式）
     *
     * @param failFast 是否快速失败：
     *                 true - 帧长度超过maxFrameLength立即抛出异常
     *                 false - 等待后续数据到达再判断
     */
    public TunnelMsgDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
                            int initialBytesToStrip, boolean failFast) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, failFast);
    }

    /**
     * 解码字节流为隧道消息对象
     *
     * @param ctx Channel处理器上下文
     * @param in2 原始输入缓冲区
     * @return 解码后的TunnelMsg对象，数据不足时返回null
     * @throws Exception 解码过程中发生的异常
     */
    @Override
    protected MessageBody decode(ChannelHandlerContext ctx, ByteBuf in2) throws Exception {
        ByteBuf in = (ByteBuf) super.decode(ctx, in2);
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < 4) {
            return null;
        }

        MessageBody messageBody = null;
        byte[] headerBytes = new byte[MessageHeader.HEADER_LENGTH];
        in.readBytes(headerBytes);
        MessageHeader messageHeader = new MessageHeader().decode(headerBytes);

        MessageType messageType = messageHeader.getType();
        if(messageType==MessageType.AUTH_REQUEST){
            int dataLength = messageHeader.getLength();
            byte[] keys = new byte[dataLength];
            in.readBytes(keys);
            TunnelAuthMsg tunnelAuthMsg = new TunnelAuthMsg();
            TunnelAuthMsg authMsg = tunnelAuthMsg.decode(keys);
            return tunnelAuthMsg;
        }else {
            TunnelMsg tunnelMsg = new TunnelMsg();
            int dataLength = messageHeader.getLength();
            byte[] data = new byte[dataLength];
            return tunnelMsg.decode(data);

        }
    }
}