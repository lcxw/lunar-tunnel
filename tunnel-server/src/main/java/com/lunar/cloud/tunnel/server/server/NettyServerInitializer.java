package com.lunar.cloud.tunnel.server.server;

import com.lunar.cloud.tunnel.server.config.ConfigContext;
import com.lunar.cloud.tunnel.server.inbound.MixinSelectHandler;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * @author kdyzm
 * @date 2021/5/14
 */
@Slf4j
@RequiredArgsConstructor
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private final EventLoopGroup clientWorkGroup;

    private final ConfigContext configProperties;


    /**
     * 根据不同的端口号创建不同的pipeline
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        InetSocketAddress localAddress = ch.localAddress();
        InetSocketAddress remoteAddress = ch.remoteAddress();
        int localPort = ch.localAddress().getPort();
        ChannelPipeline pipeline = ch.pipeline();
        log.info("添加日志请求handler");
        pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
        log.info("添加http与socks5请求解析handler");
        pipeline.addLast(new MixinSelectHandler(configProperties, clientWorkGroup));
        pipeline.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                // 处理异常
                log.error("链接:{}出现异常", ctx, cause);
//                ctx.fireChannelRead();
                ctx.flush();
//                ctx.close();
            }
        });
        log.info("local address:{},remote address{}", localAddress, remoteAddress);
        //处理socks5协议
        if (localPort == configProperties.getServerConfig().getSocksPort()) {
            log.info("客户端链接了socket5端口");
            //处理http协议
        } else {
            log.info("客户端链接了http端口");
        }
    }
}
