package com.lunar.cloud.tunnel.server.server.cloud;

import com.lunar.cloud.tunnel.core.constant.Constant;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomExternalHandler extends ChannelInboundHandlerAdapter {
    private final int externalPort;

    public CustomExternalHandler(int externalPort) {
        this.externalPort = externalPort;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 记录新的客户端连接
//        Constant.externalPortToClientMap.put(externalPort, ctx.channel());
        log.info("New client connected on port: {}", externalPort);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 移除断开的客户端连接
//        Constant.externalPortToClientMap.remove(externalPort);
        log.info("Client disconnected on port: {}", externalPort);
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 根据端口转发请求到对应的客户端
        Channel targetChannel = Constant.externalPortToClientMap.get(externalPort);
        if (targetChannel != null && targetChannel.isActive()) {
            log.info("转发服务器消息到客户端: {}-{}", externalPort,targetChannel.remoteAddress());
            targetChannel.writeAndFlush(msg);
        } else {
            log.warn("No active client found for port: {}", externalPort);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error", cause.fillInStackTrace());
        ctx.close();
    }
}
