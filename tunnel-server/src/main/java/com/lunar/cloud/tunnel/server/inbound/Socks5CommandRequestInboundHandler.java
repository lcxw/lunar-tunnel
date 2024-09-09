package com.lunar.cloud.tunnel.server.inbound;


import com.lunar.cloud.tunnel.server.config.ConfigContext;
import com.lunar.cloud.tunnel.server.encoder.TrojanRequestEncoder;
import com.lunar.cloud.tunnel.server.enums.ProxyModel;
import com.lunar.cloud.tunnel.core.util.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.AttributeKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * @author kdyzm
 * @date 2021-04-23
 */
@Slf4j
@RequiredArgsConstructor
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {
    public static final AttributeKey<String> SESSION =
        AttributeKey.valueOf("session");
    private final EventLoopGroup eventExecutors;


    private final ConfigContext configProperties;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        Channel channel = ctx.channel();
        Socks5AddressType socks5AddressType = msg.dstAddrType();
        ProxyModel pacMode = configProperties.getProxyMode(msg.dstAddr());
        if (channel.attr(SESSION).get()!=null) {
            String session = channel.attr(SESSION).get();
            log.info("session:{}", session);
        } else {
            channel.attr(SESSION).set("session");
        }
        log.debug("准备连接目标服务器，ip={},port={}", msg.dstAddr(), msg.dstPort());
        Bootstrap bootstrap = new Bootstrap();
        bootstrap = bootstrap.group(eventExecutors)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true);
        log.info("current proxy mode:{}", pacMode);
        switch (pacMode) {
            case GLOBAL -> proxyConnect(ctx, msg, socks5AddressType, bootstrap);
            case REJECT -> {
                log.info("{} 地址在黑名单中，拒绝连接", msg.dstAddr());
                //假装连接成功
                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                ctx.writeAndFlush(commandResponse);
                ctx.pipeline().addLast("HttpServerCodec", new HttpServerCodec());
                ctx.pipeline().addLast(new BlackListInboundHandler());
                ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
            }
            case DIRECT -> directConnect(ctx, msg, socks5AddressType, bootstrap);
            case PAC -> directConnect(ctx, msg, socks5AddressType, bootstrap);
            default -> log.error("无法支持的代理模式：{}", configProperties.getServerConfig().getProxyModel());

        }

    }

    private void directConnect(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg, Socks5AddressType socks5AddressType, Bootstrap bootstrap) {
        log.info("[direct][socks5] {}:{}", msg.dstAddr(), msg.dstPort());
        ChannelFuture future;
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                //添加服务端写客户端的Handler
                ch.pipeline().addLast(new Dest2ClientInboundHandler(ctx));
            }
        });
        future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.debug("目标服务器:{}:{},socketType:{},type:{}连接成功", msg.dstAddr(), msg.dstPort(), msg.dstAddrType(), msg.type());
                    //添加客户端转发请求到服务端的Handler
                    ctx.pipeline().addLast(new Client2DestInboundHandler(future));
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                    ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                    ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
                } else {
                    log.error("连接目标服务器失败,address={},port={}", msg.dstAddr(), msg.dstPort());
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                    future.channel().close();
                }
            }
        });
    }

    private void proxyConnect(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg, Socks5AddressType socks5AddressType, Bootstrap bootstrap) {
        final String dstAddr = msg.dstAddr();
        final int dstPort = msg.dstPort();
        log.info("[proxy][socks5] {}:{}", dstAddr, dstPort);
        ChannelFuture future;
        if (true) {
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    //添加服务端写客户端的Handler
                    ch.pipeline().addLast(new Dest2ClientInboundHandler(ctx));
                }
            });
            final Channel inboundChannel = ctx.channel();
            future = bootstrap.connect(dstAddr, dstPort).addListener((ChannelFutureListener) f -> {
                if (f.isSuccess()) {
                    log.debug("目标服务器连接成功");
                    //添加客户端转发请求到服务端的Handler
                    ctx.pipeline().addLast(new Client2DestInboundHandler(f));
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                    ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                    ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
                } else {
                    log.error("连接目标服务器失败,address={},port={}", msg.dstAddr(), msg.dstPort());
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                    f.channel().close();
                }
            });
        } else {
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(SslUtil.getContext().newHandler(ch.alloc()));
                    ch.pipeline().addLast(new TrojanRequestEncoder());
                }
            });
            future = bootstrap.connect(configProperties.getServerConfig().getListen(), configProperties.getServerConfig().getSocksPort());
        }

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.debug("代理服务器连接成功");
                    future.channel().pipeline().addLast(new TrojanDest2ClientInboundHandler(ctx));
                    //添加客户端转发请求到服务端的Handler
                    ctx.pipeline().addLast(new TrojanClient2DestInboundHandler(
                            future,
                            dstAddr,
                            dstPort,
                            socks5AddressType,
                            configProperties.getServerConfig().getPassword()
                        )
                    );
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                } else {
                    log.error("代理服务器连接失败,address={},port={}", dstAddr, dstPort);
                    DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                    ctx.writeAndFlush(commandResponse);
                    future.channel().close();
                }
            }
        });
    }

}
