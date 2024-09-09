package com.lunar.cloud.tunnel.server.server;

import com.lunar.cloud.tunnel.server.config.ConfigContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.ResourceLeakDetector;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @author kdyzm
 * @date 2021/5/14
 */
@Component
@Slf4j
@AllArgsConstructor
public class NettyServer {

    private final ConfigContext configContext;


    @Async
    public void start() {
        log.info("开发环境，设置资源泄漏检测级别为最高级:{}", ResourceLeakDetector.Level.PARANOID);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);
        EventLoopGroup clientWorkGroup = new NioEventLoopGroup();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            log.info("lunar proxy server config:{}", configContext);
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 512)
                // reuse the port
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new NettyServerInitializer(clientWorkGroup, configContext));
            ChannelFuture socks5Future = bootstrap.bind(configContext.getServerConfig().getSocksPort()).sync();
            log.info("socks5 netty server has started on port {}", configContext.getServerConfig().getSocksPort());
            ChannelFuture httpFuture = bootstrap.bind(configContext.getServerConfig().getHttpPort()).sync();
            log.info("http netty server has started on port {}", configContext.getServerConfig().getHttpPort());
            socks5Future.channel().closeFuture().sync();
            httpFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("netty server startup failed", e);
            throw new IllegalStateException("netty server startup failed");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
