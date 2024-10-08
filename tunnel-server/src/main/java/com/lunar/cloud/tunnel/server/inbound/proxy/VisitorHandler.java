package com.lunar.cloud.tunnel.server.inbound.proxy;


import java.util.UUID;


import com.lunar.cloud.tunnel.core.constant.Constant;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.StringUtil;

public class VisitorHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 访客连接上代理服务器了
        Channel visitorChannel = ctx.channel();
        // 先不读取访客数据
        visitorChannel.config().setOption(ChannelOption.AUTO_READ, false);

        // 生成访客ID
        String vid = UUID.randomUUID().toString();

        // 绑定访客通道
        visitorChannel.attr(Constant.VID).set(vid);
        Constant.vvc.put(vid, visitorChannel);

        TunnelMsg TunnelMsg = new TunnelMsg();
        TunnelMsg.setType(TunnelMsg.TYPE_CONNECT);
        TunnelMsg.setData(vid.getBytes());
        Constant.clientChannel.writeAndFlush(TunnelMsg);

        super.channelActive(ctx);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        TunnelMsg TunnelMsg = new TunnelMsg();
        TunnelMsg.setType(TunnelMsg.TYPE_TRANSFER);
        TunnelMsg.setData(bytes);

        // 代理服务器发送数据到客户端了
        Channel clientChannel = Constant.vcc.get(vid);
        clientChannel.writeAndFlush(TunnelMsg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelInactive(ctx);
            return;
        }
        Channel clientChannel = Constant.vcc.get(vid);
        if (clientChannel != null && clientChannel.isActive()) {

            clientChannel.config().setOption(ChannelOption.AUTO_READ, true);

            // 通知客户端，访客连接已经断开
            TunnelMsg TunnelMsg = new TunnelMsg();
            TunnelMsg.setType(TunnelMsg.TYPE_DISCONNECT);
            TunnelMsg.setData(vid.getBytes());
            clientChannel.writeAndFlush(TunnelMsg);
        }
        Constant.clearVccVvc(vid);
        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

        Channel visitorChannel = ctx.channel();
        String vid = visitorChannel.attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelWritabilityChanged(ctx);
            return;
        }
        Channel clientChannel = Constant.vcc.get(vid);
        if (clientChannel != null) {
            clientChannel.config().setOption(ChannelOption.AUTO_READ, visitorChannel.isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }
}