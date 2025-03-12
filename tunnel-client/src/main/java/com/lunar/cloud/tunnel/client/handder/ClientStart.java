package com.lunar.cloud.tunnel.client.handder;

import com.lunar.cloud.tunnel.client.constant.Constant;
import com.lunar.cloud.tunnel.client.constant.TunnelClientConfig;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgDecoder;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClientStart {
    private EventLoopGroup group;
    @Autowired
    private TunnelClientConfig tunnelClientConfig;
    private static final int RETRY_INTERVAL = 5; // 重试间隔秒数
    private Channel activeChannel; // 新增连接状态跟踪
    ScheduledFuture<?> scheduledFuture;
    private LocalDateTime lalastConnectionTime;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void initNettyClient() throws Exception {
        group = new NioEventLoopGroup();
        connectWithRetry();
    }

    private void connectWithRetry() {
        log.info("开始连接服务器");
        // 如果已有活跃连接则先关闭
        if (activeChannel != null && activeChannel.isActive() && lalastConnectionTime.withMinute(5).isAfter(LocalDateTime.now())) {
            log.info("5分钟超时关闭");
            activeChannel.close();
        }
        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(new TunnelMsgDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
                        pipeline.addLast(new TunnelMsgEncoder());
                        pipeline.addLast(new IdleStateHandler(40, 600, 0));
                        pipeline.addLast(new ProxyHandler(tunnelClientConfig));
                    }
                });
//        proxySocket.connectProxyServer(null);

        b.connect(tunnelClientConfig.getServerIp(), tunnelClientConfig.getServerPort()).addListener((ChannelFuture future) -> {
            Channel channel = future.channel();
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                switch (cause) {
                    case ClosedChannelException closedChannelException -> log.warn("客户端主动断开连接");
                    case ConnectException connectException -> log.error("服务端拒绝连接");
                    case IOException ioException -> log.error("网络异常断开: {}", cause.getMessage());
                    case null, default -> log.error("未知断开原因", cause);
                }
                // 连接失败立即抛出异常终止应用
                log.error("服务器连接失败，" + RETRY_INTERVAL + "秒后重试...", future.cause());
                // 通过通道属性判断主动关闭
                if (this.activeChannel != null) {
                    Attribute<Boolean> booleanAttribute = activeChannel.attr(AttributeKey.<Boolean>valueOf("CLIENT_CLOSE"));
                    if (booleanAttribute.get() != null) {
                        Boolean clientClose = booleanAttribute.get();
                        if (clientClose != null && clientClose) {
                            log.info("客户端主动发起的关闭");
                        }
                    }

                }

                group.schedule(this::connectWithRetry, RETRY_INTERVAL, TimeUnit.SECONDS);

            }
            if (future.isSuccess()) {
                // 连接服务器成功后，取消定期重试
                // 取消正在进行的重试任务
                if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
                    scheduledFuture.cancel(false);
                    scheduledFuture = null;
                }

                // 记录当前活跃连接
                activeChannel = future.channel();
                log.info("服务器连接成功");
                // 连接成功后发送注册消息
//                activeChannel.writeAndFlush("register:8001:127.0.0.1:8777");
                // 告诉服务端这条连接是client的连接
                TunnelMsg TunnelMsg = new TunnelMsg();
                TunnelMsg.setType(TunnelMsg.TYPE_CONNECT);
                TunnelMsg.setData(MessageFormatter.arrayFormat("client:{}:{}", new Object[]{ tunnelClientConfig.getPortalPort(),tunnelClientConfig.getToken()}).getMessage().getBytes());
                channel.writeAndFlush(TunnelMsg);

                Constant.proxyChannel = channel;
                lalastConnectionTime = LocalDateTime.now();
            }

            if (activeChannel != null) {

                // 添加连接关闭监听器实现重连
                activeChannel.closeFuture().addListener(closeFuture -> {
                    log.error("连接断开，清理资源后重连...", closeFuture.cause());
                    activeChannel = null;  // 清除失效连接
                    // 延迟重试，5秒后重试一次
                    scheduledFuture = group.scheduleWithFixedDelay(this::connectWithRetry, RETRY_INTERVAL, RETRY_INTERVAL, TimeUnit.SECONDS);
                    // 定期重试
//                group.scheduleAtFixedRate(this::connectWithRetry, 3, 3, TimeUnit.SECONDS);
                });
            }

        });
    }

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            if (null != args && args.getNonOptionArgs().size() == 4) {
                log.info("提供了客户端启动参数，替换当前参数为：{}", args.getNonOptionArgs());
                int realPort = Integer.parseInt(args.getNonOptionArgs().get(3));
                int serverPort = Integer.parseInt(args.getNonOptionArgs().get(1));
                tunnelClientConfig.setServerIp(args.getSourceArgs()[0]);
                tunnelClientConfig.setServerPort(serverPort);
                tunnelClientConfig.setRealServerIp(args.getSourceArgs()[2]);
                tunnelClientConfig.setRealPort(realPort);

            } else if (null != args && !args.getNonOptionArgs().isEmpty() && args.getNonOptionArgs().size() != 4) {
                throw new IllegalArgumentException("客户端启动参数有误");
            }
            log.info("当前客户端配置:服务器地址{}，服务器端口:{},真实服务地址:{}真实端口:{} ", tunnelClientConfig.getServerIp(), tunnelClientConfig.getServerPort(), tunnelClientConfig.getRealServerIp(), tunnelClientConfig.getRealPort());

        };
    }


    @PreDestroy
    public void stopNetty() {
        // 关闭服务
        group.shutdownGracefully();
    }

}