package com.lunar.cloud.tunnel.server.inbound.socks5;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/*
 * socks5 UDP_ASSOCIATE 请求建立成功后，记录客户端的udp消息发送端口及服务端的udp接收端口
 * */

@Slf4j
@RequiredArgsConstructor
public class Socket5DatagramInitialChannelHandler4Client2ProxyServer extends ChannelInboundHandlerAdapter {
    public static final AttributeKey<Integer> UDP_SESSION_CLIENT_PORT =
            AttributeKey.valueOf("session");
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
            // 解析客户端请求转发的udp包请求头
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
            // 客户端请求转发到的目标服务器地址信息，ip及端口
            String proxyTargetIp = clientAddress.getHostAddress();
            int proxyTargetPort = ByteBuffer.wrap(clientPortBytes).getShort() & 0xFFFF;
            // 客户端请求转发到的目标服务器的udp包信息
            byte[] actualClientData = Arrays.copyOfRange(receivedData, 10, receivedData.length);
            // 客户端发送过来的数据，既客户端像真实服务器发送的数据信息
            String clientDataString = new String(actualClientData);
            log.info("receive client proxy request, want's to send data:{} to target:[{}：{}]", clientDataString, proxyTargetIp, proxyTargetPort);
            // 使用代理服务器，既socks5 的udp服务器新建端口像真实服务器发送数据
            log.info("begin to transform client data:{} to actual remote at[{}:{}]", clientDataString, proxyTargetIp, proxyTargetPort);
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
                                ch.pipeline().addLast(new Socket5DatagramInitialChannelHandler4Proxy2ClientServer(port, proxyTargetIp, protolHeader, clientIpBytes, clientPortBytes, ctx));
                            }
                        })
                        .option(ChannelOption.SO_BROADCAST, true);
                Channel serverChannel = null;
                // 3. 使用随机端口发送数据到远程服务器并接收返回数据
                Attribute<Integer> attr = ctx.channel().attr(UDP_SESSION_CLIENT_PORT);
                int udpProxy2ServerPort;
                if (attr != null && attr.get() != null) {
                    udpProxy2ServerPort = attr.get();
                    serverChannel = bootstrap.bind(udpProxy2ServerPort).sync().channel();
                } else {
                    udpProxy2ServerPort = 0;
                    serverChannel = bootstrap.bind(udpProxy2ServerPort).sync().channel();
                }
                // 4. 获取绑定的端口号
                int clientSenderPort = ((InetSocketAddress) serverChannel.localAddress()).getPort();
                ctx.channel().attr(UDP_SESSION_CLIENT_PORT).set(clientSenderPort);
                log.error("udp client started on clientSenderPort: " + clientSenderPort);

                // 发送UDP数据
                log.info("发送udp数据到真实服务器");
                DatagramPacket remotePacket = new DatagramPacket(Unpooled.wrappedBuffer(actualClientData), new InetSocketAddress(InetAddress.getByName(proxyTargetIp), proxyTargetPort));
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