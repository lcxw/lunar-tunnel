package com.lunar.cloud.tunnel.client.handder;


import com.lunar.cloud.tunnel.client.constant.Constant;
import com.lunar.cloud.tunnel.client.constant.TunnelClientConfig;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.lunar.cloud.tunnel.core.protocol.TunnelMsg.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProxyHandler extends SimpleChannelInboundHandler<TunnelMsg> {
    private final TunnelClientConfig tunnelClientConfig;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, TunnelMsg tunnelMsg) {
        // 客户端读取到代理过来的数据了
        log.info("客户端读取到代理过来的数据了:{}", tunnelMsg);
        byte type = tunnelMsg.getType();
        String vid = new String(tunnelMsg.getData());
        switch (type) {
            case TYPE_HEARTBEAT:
                log.info("收到服务端心跳包，忽略");
                break;
            case TYPE_CONNECT:
                log.info("收到服务端连接请求，vid:{},开始连接到目标真实服务:{}", vid, tunnelClientConfig);
                RealSocket.connectRealServer(vid, tunnelClientConfig);
                break;
            case TYPE_DISCONNECT:
                // 断开连接
                log.info("收到服务端断开连接请求，vid:{},开始断开连接", vid);
                Constant.clearvpcvrcAndClose(vid);
                break;
            case TYPE_TRANSFER:
                // 把数据转到真实服务
                log.info("收到服务端转发数据请求，vid:{},开始转发数据", vid);
                ByteBuf buf = ctx.alloc().buffer(tunnelMsg.getData().length);
                buf.writeBytes(tunnelMsg.getData());

                String visitorId = ctx.channel().attr(Constant.VID).get();
                Channel rchannel = Constant.vrc.get(visitorId);
                if (null != rchannel) {
                    rchannel.writeAndFlush(buf);
                }else{
                    log.error("未找到真实服务通道");
                }
                break;
            default:
                // 操作有误
        }
        // 客户端发数据到真实服务了
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelWritabilityChanged(ctx);
            return;
        }
        Channel realChannel = Constant.vrc.get(vid);
        if (realChannel != null) {
            realChannel.config().setOption(ChannelOption.AUTO_READ, ctx.channel().isWritable());
        }

        super.channelWritabilityChanged(ctx);
    }

    /**
     * 连接成功后，把vid绑定到channel上
     *
     * @param ctx 上下文
     * @throws Exception 异常
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String vid = ctx.channel().attr(Constant.VID).get();
        if (StringUtil.isNullOrEmpty(vid)) {
            super.channelInactive(ctx);
            return;
        }
        Channel realChannel = Constant.vrc.get(vid);
        if (realChannel != null && realChannel.isActive()) {
            realChannel.close();
        }
        super.channelInactive(ctx);
    }

    /**
     * 连接异常后，关闭连接
     *
     * @param ctx   上下文
     * @param cause 异常
     * @throws Exception 异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error("连接异常即将关闭: ", cause);
        ctx.channel().attr(AttributeKey.valueOf("CLOSE_REASON")).set(cause);
        ctx.close();
    }

    /**
     * 当通道触发用户事件时，此方法会被调用。主要用于处理空闲状态事件。
     *
     * @param ctx 通道处理上下文，提供了与通道相关的操作和状态信息。
     * @param evt 触发的用户事件对象。
     * @throws Exception 处理事件过程中可能抛出的异常。
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    // 读超时，关闭连接
                    log.error("读超时即将关闭连接:{}", ctx.channel().remoteAddress());
                    ctx.channel().close();
                    break;
                case WRITER_IDLE:
                    // 写超时，发送心跳包
                    log.info("写超时即将发送心跳包:{}", ctx.channel().remoteAddress());
                    TunnelMsg tunnelmsg = new TunnelMsg();
                    tunnelmsg.setType(TunnelMsg.TYPE_HEARTBEAT);
                    ctx.channel().writeAndFlush(tunnelmsg);
                    break;
                case ALL_IDLE:
                    // 读写超时，关闭连接
                    log.error("读写超时即将关闭连接:{}", ctx.channel().remoteAddress());
                    ctx.channel().close();
                    break;
            }
        }
    }
}