package com.lunar.cloud.tunnel.server.server.cloud;

import com.lunar.cloud.tunnel.core.constant.Constant;
import com.lunar.cloud.tunnel.core.constant.PortMapping;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CloudServer {
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initCloudServer() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            ch.pipeline().addLast(new TrafficStatisticsHandler(1000)); // 1000ms check interval
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            p.addLast(new CloudServerHandler());
//                            p.addLast(new ReverseTunnelDataTransforHandler());
                        }
                    });

            ChannelFuture f = b.bind(28080).sync();
            // 代理配置
            List<PortMapping> mappings = new ArrayList<>();
            mappings.add(new PortMapping(8000, "127.0.0.1", 80));
            mappings.add(new PortMapping(8002, "127.0.0.1", 7777));
            Constant.TunnelConfig.addAll(mappings);
            EventLoopGroup externalBossGroup = new NioEventLoopGroup();
            EventLoopGroup externalWorkerGroup = new NioEventLoopGroup();
            for (PortMapping mapping : mappings) {
                String key = mapping.getInternalHost() + ":" + mapping.getInternalPort();
                int externalPort = mapping.getExternalPort();
                log.info("register:{} to :{}", key, externalPort);
                // 对每个服务端端口，启动一个服务用于监听链接并转发数据到对应的客户局
                ServerBootstrap serverBootstrap = new ServerBootstrap().group(externalBossGroup, externalWorkerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {

                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();
//                                Constant.externalPortToClientMap.putIfAbsent(externalPort, ch);
                                ch.pipeline().addLast(new TrafficStatisticsHandler(1000)); // 1000ms check interval
                                ch.pipeline().addLast(new StringDecoder()); // 1000ms check interval
                                ch.pipeline().addLast(new StringEncoder()); // 1000ms check interval
                                // 实现一个ChannelPipeline用于当有新连接连接时，将请求转发到对应的客户端，实现代理功能，实现内网穿透
                                p.addLast(new CustomExternalHandler(externalPort));

                            }
                        });
                serverBootstrap.bind(externalPort).sync();


            }
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
