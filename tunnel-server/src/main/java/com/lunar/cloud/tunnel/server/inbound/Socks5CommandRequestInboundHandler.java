package com.lunar.cloud.tunnel.server.inbound;


import com.lunar.cloud.tunnel.server.config.ConfigContext;
import com.lunar.cloud.tunnel.server.encoder.TrojanRequestEncoder;
import com.lunar.cloud.tunnel.server.enums.ProxyModel;
import com.lunar.cloud.tunnel.core.util.SslUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;


/**
 * @author kdyzm
 * @date 2021-04-23
 */
@Slf4j
@RequiredArgsConstructor
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {
    public static final AttributeKey<Integer> UDP_SESSION_CLIENT_PORT =
            AttributeKey.valueOf("session");
    private final EventLoopGroup eventExecutors;


    private final ConfigContext configProperties;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) {
        Channel channel = ctx.channel();
        Socks5AddressType socks5AddressType = msg.dstAddrType();
        ProxyModel pacMode = configProperties.getProxyMode(msg.dstAddr());

        Socks5CommandType socks5CommandType = msg.type();
        log.debug("收到客户端请求，ip={},port={},目标服务器地址类型:{},流量协议类型:{}", msg.dstAddr(), msg.dstPort(), socks5AddressType, socks5CommandType);
        switch (socks5CommandType.byteValue()) {

            case 0x03: {
                EventLoopGroup group = new NioEventLoopGroup();
                log.info("udp msg");
                Bootstrap bootstrap = new Bootstrap();
                try {
                    bootstrap.group(group)
                            .channel(NioDatagramChannel.class)
                            .option(ChannelOption.SO_REUSEADDR, true)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

                    bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
                                @Override
                                protected void initChannel(NioDatagramChannel ch) throws Exception {
                                    ch.pipeline().addFirst(new LoggingHandler("udpLoggingHandler"));
                                    ch.pipeline().addLast(new Socket5DatagramInitialChannelHandler(msg.dstPort()));
                                }
                            })
                            .option(ChannelOption.SO_BROADCAST, true);
                    if (channel.attr(UDP_SESSION_CLIENT_PORT).get() != null) {
                        Integer session = channel.attr(UDP_SESSION_CLIENT_PORT).get();
                        log.info("udp session: port{}", session);
                    } else {
                        log.info("save udp session port:{}", msg.dstPort());
                        channel.attr(UDP_SESSION_CLIENT_PORT).set(msg.dstPort());
                    }
                    // 3. 绑定端口
                    Channel serverChannel = bootstrap.bind(0).sync().channel();
                    // 4. 获取绑定的端口号
                    int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();
                    log.error("UDP Server started on port: " + port);
                    channel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4, "127.0.0.1", port));
                    ctx.fireChannelActive();
                    // 等待 Channel 关闭
                    serverChannel.closeFuture().sync();
                } catch (Exception e) {
                    log.error("udp msg error", e);
                } finally {
                    group.shutdownGracefully();
                }
                break;
            }
            case 0x02: {
                log.info("bind msg");
                break;
            }
            case 0x01: {
                log.info("connect msg");
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
                break;
            }
            default: {
                break;
            }
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

    @RequiredArgsConstructor
    static class Socket5DatagramInitialChannelHandler extends ChannelInboundHandlerAdapter {
        private final int port;


        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            Integer udpSessionPort = ctx.channel().attr(UDP_SESSION_CLIENT_PORT).get();
            if (udpSessionPort != null) {
                log.info("udp udpSessionPort: port{}", udpSessionPort);
            }
            DatagramPacket packet = (DatagramPacket) msg;
            ByteBuf content = packet.content();

            // 读取数据
            byte[] receivedData = new byte[content.readableBytes()];
            content.readBytes(receivedData);
            String receivedMessage = new String(receivedData, CharsetUtil.UTF_8);
            log.info("Received client sockst packaged message:[{}] from:{} at:{}", receivedMessage, packet.sender(), packet.recipient());
            try {
                // 解析请求头
                byte[] protolHeader = Arrays.copyOfRange(receivedData, 0, 4);
                byte[] reversionByte = Arrays.copyOfRange(protolHeader, 0, 2);
                int reversion = ByteBuffer.wrap(reversionByte).getShort() & 0xFFFF;
                byte[] fragByte = Arrays.copyOfRange(protolHeader, 2, 3);
                int frag = ByteBuffer.wrap(fragByte).get() & 0xFFFF;
                byte[] addrTypeByte = Arrays.copyOfRange(protolHeader, 3, 4);
                int addrType = ByteBuffer.wrap(addrTypeByte).get() & 0xFFFF;
                log.info("socks 5 udp relay message received,reversion:{},frag:{},addrType:{}", reversion, frag, addrType);
                byte[] clientIpBytes = Arrays.copyOfRange(receivedData, 4, 8);
                byte[] clientPortBytes = Arrays.copyOfRange(receivedData, 8, 10);
                InetAddress clientAddress = InetAddress.getByAddress(clientIpBytes);
                String clientIp = clientAddress.getHostAddress();
                int clientPort = ByteBuffer.wrap(clientPortBytes).getShort() & 0xFFFF;
                byte[] actualClientData = Arrays.copyOfRange(receivedData, 10, receivedData.length);
                // 客户端发送过来的数据，既客户端像真实服务器发送的数据信息
                String clientDataString = new String(actualClientData);
                log.info("receive client data:{} from[{}：{}]", clientDataString, clientIp, clientPort);
                log.info("begin to transform client data:{} to actual remote at[{}:{}]", clientDataString, clientIp, clientPort);

                // 使用代理服务器，既socks5 的udp服务器新建端口像真实服务器发送数据
                log.info("begin to send udp msg to actual server");
                try {
                    EventLoopGroup group = new NioEventLoopGroup();
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(group)
                            .channel(NioDatagramChannel.class)
                            .option(ChannelOption.SO_REUSEADDR, true)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);
                    bootstrap.handler(new ChannelInitializer<NioDatagramChannel>() {
                                @Override
                                protected void initChannel(NioDatagramChannel ch) throws Exception {
                                    ch.pipeline().addFirst(new LoggingHandler("udpProxyHandler"));
                                    ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelRead(ChannelHandlerContext remoteCtx, Object msg) throws Exception {
                                            DatagramPacket packet = (DatagramPacket) msg;
                                            ByteBuf content = packet.content();
                                            // 读取数据
                                            byte[] receivedServerData = new byte[content.readableBytes()];
                                            content.readBytes(receivedServerData);
                                            String receivedMessage = new String(receivedServerData, CharsetUtil.UTF_8);
                                            log.info("Received message:[{}] from:{} at:{}", receivedMessage, packet.sender(), packet.recipient());
                                            Channel remoteChannel = remoteCtx.channel();

                                            ByteBuf buffer = Unpooled.buffer();
                                            buffer.writeBytes(protolHeader);
                                            buffer.writeBytes(clientIpBytes);
                                            buffer.writeBytes(clientPortBytes);
                                            buffer.writeBytes(receivedServerData);

//                                            DatagramPacket rePacket = new DatagramPacket(Unpooled.wrappedBuffer(receivedServerData), (InetSocketAddress) ctx.channel().localAddress());
                                            DatagramPacket rePacket = new DatagramPacket(Unpooled.wrappedBuffer(receivedServerData), new InetSocketAddress("127.0.0.1", port));


//                                            DatagramPacket rePacket2 = new DatagramPacket(buffer, new InetSocketAddress("127.0.0.1", clientPort));
                                            DatagramPacket rePacket2 = new DatagramPacket(Unpooled.wrappedBuffer(buffer), new InetSocketAddress("127.0.0.1", port));


                                            byte[] result = new byte[buffer.readableBytes()];
                                            buffer.readBytes(result);
                                            log.info("准备发送给客户端的数据:{}", result);
//                                            ctx.channel().writeAndFlush(rePacket).sync();
                                            ctx.channel().writeAndFlush(rePacket2).sync();
//                                            ch.flush();
//                                            ctx.fireChannelActive();
//                                            content.release();
                                            // 等待返回数据
//                                            remoteChannel.closeFuture().sync();
//                                            remoteCtx.close().awaitUninterruptibly();
//                                            ctx.writeAndFlush(receivedServerData);

//                                            group.shutdownGracefully();
                                        }
                                    });
                                }
                            })
                            .option(ChannelOption.SO_BROADCAST, true);
                    // 3. 使用随机端口发送数据到远程服务器并接收返回数据
                    Channel serverChannel = bootstrap.bind(0).sync().channel();
                    // 4. 获取绑定的端口号
                    int clientSenderPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
                    log.error("udp client started on clientSenderPort: " + clientSenderPort);

                    // 发送UDP数据
                    log.info("发送udp数据到真实服务器");
                    DatagramPacket remotePacket = new DatagramPacket(Unpooled.wrappedBuffer(actualClientData), new InetSocketAddress(InetAddress.getByName(clientIp), clientPort));
                    serverChannel.writeAndFlush(remotePacket).sync();

                    // 等待返回数据
//                    serverChannel.closeFuture().sync();
//                    ctx.fireChannelActive();

                } catch (Exception e) {
                    log.error("udp msg error", e);
                } finally {
                    log.error("udp msg finish");
                }
                // 回复消息
                log.info("收到真实服务器返回的消息");
//                byte[] response = ("Hello from server: " + receivedMessage).getBytes(CharsetUtil.UTF_8);
//                DatagramPacket responsePacket = new DatagramPacket(Unpooled.wrappedBuffer(response), packet.sender());
//                ctx.writeAndFlush(responsePacket);
//                ctx.fireChannelActive();

            } catch (Exception e) {
                log.error("error parse data", e);
            } finally {
                // 释放资源
                content.release();
                // 往组播地址中发送数据报
//                ctx.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer("hello world", CharsetUtil.UTF_8), packet.sender()));
                // 关闭Channel
//                ctx.close().awaitUninterruptibly();
            }


        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("error ", cause);
            ctx.flush();
        }


    }
}
