package com.lunar.cloud.tunnel.client.inner;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntranetClientHandler extends SimpleChannelInboundHandler<String> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 处理云端服务器的消息
        if (msg.startsWith("forward:")) {
            String[] parts = msg.split(":");
            String request = parts[1];
            // 转发请求到内网服务器
            String response = forwardRequestToIntranetServer(request);
            ctx.writeAndFlush("response:" + response);
        }else{
            log.info("Received message from cloud server: {}", msg);
            ctx.writeAndFlush("response:" + "echo");

        }
    }

    private String forwardRequestToIntranetServer(String request) {
        // 实现转发请求到内网服务器的逻辑
        return "response-from-intranet-server";
    }
}
