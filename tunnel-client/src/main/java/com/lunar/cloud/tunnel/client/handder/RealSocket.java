package com.lunar.cloud.tunnel.client.handder;


import com.lunar.cloud.tunnel.client.constant.Constant;
import com.lunar.cloud.tunnel.client.constant.TunnelClientConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RealSocket {
    static EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    /**
     * 连接真实服务
     *
     * @param vid 访客ID
     * @return
     */
    public static Channel connectRealServer(String vid,TunnelClientConfig clientConfig) {
        if (StringUtil.isNullOrEmpty(vid)) {
            return null;
        }
        Channel channel = Constant.vrc.get(vid);
        if (null == channel) {
            newConnect(vid,clientConfig);
            channel = Constant.vrc.get(vid);
        }
        return channel;
    }

    private static void newConnect(String vid,TunnelClientConfig clientConfig) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new RealHandler());
                        }

                    });
            bootstrap.connect(clientConfig.getRealServerIp(), clientConfig.getRealPort()).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // 客户端链接真实服务成功
                        future.channel().config().setOption(ChannelOption.AUTO_READ, false);
                        future.channel().attr(Constant.VID).set(vid);
                        Constant.vrc.put(vid, future.channel());
                        ProxySocket.connectProxyServer(vid,clientConfig);
                    }
                }
            });
        } catch (Exception e) {
            log.error("连接真实服务失败", e);
        }
    }
}