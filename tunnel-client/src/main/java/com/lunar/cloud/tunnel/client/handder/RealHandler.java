package com.lunar.cloud.tunnel.client.handder;



import com.lunar.cloud.tunnel.client.constant.Constant;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;

public class RealHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        // 客户读取到真实服务数据了
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        TunnelMsg TunnelMsg = new TunnelMsg();
        TunnelMsg.setType(TunnelMsg.TYPE_TRANSFER);
        TunnelMsg.setData(bytes);
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        Channel proxyChannel = Constant.vpc.get(vid);
        if (null != proxyChannel) {
            proxyChannel.writeAndFlush(TunnelMsg);
        }
        // 客户端发送真实数据到代理了
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelInactive(ctx);
            return;
        }
        Channel proxyChannel = Constant.vpc.get(vid);
        if (proxyChannel != null) {
            TunnelMsg TunnelMsg = new TunnelMsg();
            TunnelMsg.setType(TunnelMsg.TYPE_DISCONNECT);
            TunnelMsg.setData(vid.getBytes());
            proxyChannel.writeAndFlush(TunnelMsg);
        }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelWritabilityChanged(ctx);
            return;
        }
        Channel proxyChannel = Constant.vpc.get(vid);
        if (proxyChannel != null) {
            proxyChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
