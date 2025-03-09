package com.lunar.cloud.tunnel.server.inbound.proxy;

import com.lunar.cloud.tunnel.core.constant.Constant;
import com.lunar.cloud.tunnel.core.constant.PortMapping;
import com.lunar.cloud.tunnel.server.config.ServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VisitorSocket {
    private final ServerConfig serverConfig;

    /**
     * 启动服务代理
     *
     * @throws Exception
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void startServer() throws Exception {
        log.info("访客服务代理启动");
        List<PortMapping> mappings = serverConfig.getReverseProxyList();
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
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new ChannelDuplexHandler());
                            pipeline.addLast(new VisitorHandler());

                        }
                    });
            serverBootstrap.bind(externalPort).sync();
        }
    }

}
