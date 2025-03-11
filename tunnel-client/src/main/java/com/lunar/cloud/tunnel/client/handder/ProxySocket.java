package com.lunar.cloud.tunnel.client.handder;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import com.lunar.cloud.tunnel.client.constant.Constant;
import com.lunar.cloud.tunnel.client.constant.TunnelClientConfig;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsg;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgDecoder;
import com.lunar.cloud.tunnel.core.protocol.TunnelMsgEncoder;
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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.hutool.extra.spring.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProxySocket {
    private static EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public static void connectProxyServer(String vid, TunnelClientConfig clientConfig) {
        if (StringUtil.isNullOrEmpty(vid)) {
            if (Constant.proxyChannel == null || !Constant.proxyChannel.isActive()) {
                newConnect(null, clientConfig);
            }
        } else {
            Channel channel = Constant.vpc.get(vid);
            if (null == channel) {
                newConnect(vid, clientConfig);
                channel = Constant.vpc.get(vid);
            }
        }
    }

    private static void newConnect(String vid, TunnelClientConfig clientConfig) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new TunnelMsgDecoder(Integer.MAX_VALUE, 0, 4, -4, 0));
                        pipeline.addLast(new TunnelMsgEncoder());
                        pipeline.addLast(new IdleStateHandler(40, 600, 0));
                        pipeline.addLast(new ProxyHandler(SpringUtil.getBean(TunnelClientConfig.class)));
                    }
                });

        bootstrap.connect(clientConfig.getServerIp(), clientConfig.getServerPort()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // 客户端链接代理服务器成功
                    Channel channel = future.channel();
                    if (StringUtil.isNullOrEmpty(vid)) {
                        // 告诉服务端这条连接是client的连接
                        TunnelMsg TunnelMsg = new TunnelMsg();
                        TunnelMsg.setType(MessageType.TYPE_CONNECT);
                        TunnelMsg.setData("client".getBytes());
                        channel.writeAndFlush(TunnelMsg);

                        Constant.proxyChannel = channel;
                    } else {

                        // 告诉服务端这条连接是vid的连接
                        TunnelMsg TunnelMsg = new TunnelMsg();
                        TunnelMsg.setType(MessageType.TYPE_CONNECT);
                        TunnelMsg.setData(vid.getBytes());
                        channel.writeAndFlush(TunnelMsg);

                        // 客户端绑定通道关系
                        Constant.vpc.put(vid, channel);
                        channel.attr(Constant.VID).set(vid);

                        Channel realChannel = Constant.vrc.get(vid);
                        if (null != realChannel) {
                            realChannel.config().setOption(ChannelOption.AUTO_READ, true);
                        }
                    }
                }
            }
        });
    }
}