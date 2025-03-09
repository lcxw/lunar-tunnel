package com.lunar.cloud.tunnel.server.inbound.proxy;

import com.lunar.cloud.tunnel.core.constant.Constant;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgDecoder;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgEncoder;
import com.lunar.cloud.tunnel.server.config.ServerConfig;
import com.lunar.cloud.tunnel.server.inbound.TrafficStatisticsHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

// todo 实现多配置代理服务器以支持多客户端
@Component
@Slf4j
@RequiredArgsConstructor
public class CloudServer {
    private final ServerConfig serverConfig;
    private ChannelFuture serverChannelFuture;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initCloudServer() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            log.info("代理服务端开始启动");
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new TrafficStatisticsHandler(1000)); // 1000ms check interval
                            pipeline.addLast(new TunnelMsgDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
                            pipeline.addLast(new TunnelMsgEncoder());
                            pipeline.addLast(new IdleStateHandler(40, 600, 0));
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            serverChannelFuture = b.bind(serverConfig.getReverseProxyRegisterPort()).sync();
            serverChannelFuture.addListener((ChannelFutureListener) channelFuture -> {
                // 服务器已启动
                log.info("服务端已启动:{}", channelFuture.channel().localAddress());
            });
            serverChannelFuture.channel().closeFuture().sync();

        } finally {
            if (serverChannelFuture != null) {
                serverChannelFuture.channel().close().syncUninterruptibly();
            }
            if (!bossGroup.isShutdown()) {
                bossGroup.shutdownGracefully();
            }
            if (!workerGroup.isShutdown()) {
                workerGroup.shutdownGracefully();
            }
        }
    }
}
