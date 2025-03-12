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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VisitorHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        // 访客连接上代理服务器了
        Channel visitorChannel = ctx.channel();
        // 先不读取访客数据
        log.info("有访客连接接入:{}", visitorChannel.remoteAddress());

        visitorChannel.config().setOption(ChannelOption.AUTO_READ, false);

        // 生成访客ID
        String vid = UUID.randomUUID().toString();

        // 绑定访客通道
        visitorChannel.attr(Constant.VID).set(vid);
        Constant.vvc.put(vid, visitorChannel);

        TunnelMsg tunnelMsg = new TunnelMsg();
        tunnelMsg.setType(TunnelMsg.TYPE_CONNECT);
        tunnelMsg.setData(vid.getBytes());
        log.info("像客户端发送连接握手信息");
        if (Constant.clientChannel == null ||
                !Constant.clientChannel.isActive()
                || Constant.clientChannelMap.isEmpty()
                || Constant.clientChannelMap.get(Integer.valueOf(ctx.channel().localAddress().toString().split(":")[1])) == null) {
            log.info("客户端未连接");
            return;
        } else {
            Channel clientChannel = Constant.clientChannelMap.get(Integer.valueOf(ctx.channel().localAddress().toString().split(":")[1]));
            if (clientChannel != null && clientChannel.isActive()) {
                clientChannel.writeAndFlush(tunnelMsg);
            } else {
                Constant.clientChannel.writeAndFlush(tunnelMsg);
            }
        }
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
        TunnelMsg tunnelMsg = new TunnelMsg();
        tunnelMsg.setType(TunnelMsg.TYPE_TRANSFER);
        tunnelMsg.setData(bytes);

        log.info("代理服务器发送数据到客户端了");
        // 代理服务器发送数据到客户端了
        Channel clientChannel = Constant.vcc.get(vid);
        clientChannel.writeAndFlush(tunnelMsg);
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
            log.info("访客已断开链接");
            // 通知客户端，访客连接已经断开
            TunnelMsg tunnelMsg = new TunnelMsg();
            tunnelMsg.setType(com.lunar.cloud.tunnel.core.protocol.TunnelMsg.TYPE_DISCONNECT);
            tunnelMsg.setData(vid.getBytes());
            clientChannel.writeAndFlush(tunnelMsg);
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
        log.error("访客连接异常", cause);
        ctx.close();
    }
}