package com.lunar.cloud.tunnel.client.socket;

import com.lunar.cloud.tunnel.client.constant.TunnelClientConfig;
import com.lunar.cloud.tunnel.client.constant.TunnelClientConstant;
import com.lunar.cloud.tunnel.client.handder.ProxyHandler;
import com.lunar.cloud.tunnel.client.handder.RealHandler;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgDecoder;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Component
public class ClientSocket {
    private final EventLoopGroup proxyEventLoopGroup = new NioEventLoopGroup();
    private final TunnelClientConfig tunnelClientConfig;

    /**
     * 重连代理服务
     */
    private final ScheduledExecutorService proxyReconnectExecutor = Executors.newSingleThreadScheduledExecutor();


    /**
     * 连接代理服务器的方法
     * <p>
     * 本方法尝试连接到代理服务器，并在连接失败时安排重连任务
     * 它使用一个定时任务来周期性地尝试重新连接到代理服务器
     *
     * @return Channel 表示与代理服务器的连接通道
     * @throws Exception 如果连接过程中出现任何异常，则抛出此异常
     */
    public Channel connectProxyServer() throws Exception {
        // 安排一个定时任务，每隔3秒尝试重连代理服务器
        proxyReconnectExecutor.scheduleAtFixedRate(() -> {
            try {
                // 尝试连接代理服务器，这里传入null作为配置参数
                connectProxyServer(null);
            } catch (Exception e) {
                // 如果连接失败，记录错误日志
                log.error("重连代理服务失败", e);
            }
        }, 3, 3, TimeUnit.SECONDS);

        // 尝试连接代理服务器并返回连接通道
        return connectProxyServer(null);
    }


    /**
     * 连接代理服务器核心方法
     * @param vid 虚拟连接标识符，为空时处理默认代理连接，非空时处理特定vid连接
     * @return Channel 代理服务器连接通道
     */
    public Channel connectProxyServer(String vid) {
        // 处理默认代理连接
        if (StringUtil.isNullOrEmpty(vid)) {
            // 检查默认通道状态，必要时创建新连接
            if (TunnelClientConstant.proxyChannelToServer == null || !TunnelClientConstant.proxyChannelToServer.isActive()) {
                newConnect(null);
            }
            return null;
        }
        // 处理特定vid连接
        else {
            Channel channel = TunnelClientConstant.vpc.get(vid);
            if (null == channel) {
                // 创建新vid连接并重新获取通道
                newConnect(vid);
                channel = TunnelClientConstant.vpc.get(vid);
            }
            return channel;
        }
    }

    /**
     * 创建新的代理服务器连接
     * @param vid 虚拟连接标识符，为空时创建默认连接，非空时创建特定vid连接
     */
    private void newConnect(String vid) {
        // 初始化Netty客户端引导程序
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(proxyEventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加自定义协议解码器（最大帧长/长度字段偏移量等配置）
                        pipeline.addLast(new TunnelMsgDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
                        // 添加自定义协议编码器
                        pipeline.addLast(new TunnelMsgEncoder());
                        // 配置读写空闲检测（40秒读超时/8秒写间隔）
                        pipeline.addLast(new IdleStateHandler(40, 8, 0));
                        // 添加代理业务处理器
                        pipeline.addLast(new ProxyHandler());
                    }
                });

        // 异步连接服务器并添加监听器
        bootstrap.connect(tunnelClientConfig.getServerIp(), tunnelClientConfig.getServerPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel channel = future.channel();
                // 处理默认连接类型
                if (StringUtil.isNullOrEmpty(vid)) {
                    // 构建客户端注册消息（类型+客户端标识）
                    TunnelMsg msg = new TunnelMsg();
                    msg.setType(TunnelMsg.TYPE_CONNECT);
                    msg.setData("client".getBytes());
                    channel.writeAndFlush(msg);

                    // 更新全局默认通道引用
                    TunnelClientConstant.proxyChannelToServer = channel;
                }
                // 处理虚拟连接类型
                else {
                    // 构建虚拟连接注册消息（类型+VID标识）
                    TunnelMsg msg = new TunnelMsg();
                    msg.setType(TunnelMsg.TYPE_CONNECT);
                    msg.setData(vid.getBytes());
                    channel.writeAndFlush(msg);

                    // 维护VID与通道的映射关系
                    TunnelClientConstant.vpc.put(vid, channel);
                    channel.attr(TunnelClientConstant.VID).set(vid);

//                    // 激活关联的真实通道读取（如果存在）
                    Channel realChannel = TunnelClientConstant.vrc.get(vid);
                    if (null != realChannel) {
                        realChannel.config().setOption(ChannelOption.AUTO_READ, true);
                    }
                }
            }
        });
    }


}
