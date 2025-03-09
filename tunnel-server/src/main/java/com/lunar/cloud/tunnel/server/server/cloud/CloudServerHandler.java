package com.lunar.cloud.tunnel.server.server.cloud;

import com.lunar.cloud.tunnel.core.constant.Constant;
import com.lunar.cloud.tunnel.core.constant.PortMapping;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;


@Slf4j
public class CloudServerHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("New client connected: {}", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        if (msg.startsWith("register:")) {
            String[] parts = msg.split(":");
            int externalServerPort = Integer.parseInt(parts[1]);
            String intranetServerAddress = parts[2];
            String intranetServerPort = parts[3];
            log.info("skip auth for client:{}  registration by :{}:{}:{}", ctx.channel().remoteAddress(), externalServerPort, intranetServerAddress, intranetServerAddress);

            Constant.externalPortToClientMap.putIfAbsent(externalServerPort, ctx.channel());
            PortMapping portMapping = new PortMapping(externalServerPort, intranetServerAddress, Integer.parseInt(intranetServerPort));
            Constant.TunnelConfig.add(portMapping);
            // 将externalServerPort变量保存到当前的context/channel中
            Attribute<Integer> externalServerPortAttr = ctx.channel().attr(AttributeKey.valueOf("externalServerPort"));
            externalServerPortAttr.set(externalServerPort);
            ctx.pipeline().remove(this);
        }else{
            log.info("Received message from cloud server: {}", msg);
        }
//        else if (msg.startsWith("request:")) {
//            String[] parts = msg.split(":");
//            String intranetServerAddress = parts[1];
//            String intranetServerPort = parts[2];
//            String request = parts[3];
//            Channel intranetClient = intranetClients.get(intranetServerAddress + ":" + intranetServerPort);
//            if (intranetClient != null) {
//                intranetClient.writeAndFlush("forward:" + request);
//            }
//        } else if (msg.startsWith("response:")) {
//            String response = msg.substring("response:".length());
//            // 将响应转发给普通用户客户端
//            // 这里需要维护普通用户客户端的通道信息
//        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Attribute<Integer> externalServerPortAttr = ctx.channel().attr(AttributeKey.valueOf("externalServerPort"));
        Integer port = externalServerPortAttr.get();
        log.info("客户端断开链接:{}", port);
        if(port!=null){
            Constant.externalPortToClientMap.remove(port);
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("error", cause.fillInStackTrace());
        ctx.close();
    }
}
