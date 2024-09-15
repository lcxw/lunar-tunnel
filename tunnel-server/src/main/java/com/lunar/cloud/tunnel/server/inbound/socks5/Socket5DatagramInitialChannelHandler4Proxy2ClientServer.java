package com.lunar.cloud.tunnel.server.inbound.socks5;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

/**
 * 收到服务器回复的udp数据包后，需要将收到的udp包返回给客户端
 */
@RequiredArgsConstructor
@Slf4j
public class Socket5DatagramInitialChannelHandler4Proxy2ClientServer extends ChannelInboundHandlerAdapter {
    private final int clientPort;
    private final String proxyTargetIp;
    private final byte[] protolHeader;
    private final byte[] clientIpBytes;
    private final byte[] clientPortBytes;
    private final ChannelHandlerContext clientChannelContext;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket packet = (DatagramPacket) msg;
        ByteBuf content = packet.content();
        // 读取数据
        byte[] receivedServerData = new byte[content.readableBytes()];
        content.readBytes(receivedServerData);
        String receivedMessage = new String(receivedServerData, CharsetUtil.UTF_8);
        log.info("Received message:[{}] from:{} at:{}", receivedMessage, packet.sender(), packet.recipient());
        Channel remoteChannel = ctx.channel();

        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(protolHeader);
        buffer.writeBytes(clientIpBytes);
        buffer.writeBytes(clientPortBytes);
        buffer.writeBytes(receivedServerData);


        DatagramPacket rePacket2 = new DatagramPacket(Unpooled.wrappedBuffer(buffer), new InetSocketAddress(proxyTargetIp, clientPort));
        log.info("准备发送给客户端:[{}:{}]的数据:{}", proxyTargetIp, clientPort, receivedServerData);
//                                            ctx.channel().writeAndFlush(rePacket).sync();
        clientChannelContext.channel().writeAndFlush(rePacket2).sync();
//                                            ch.flush();
//                                            ctx.fireChannelActive();
//                                            content.release();
        // 等待返回数据
//                                            remoteChannel.closeFuture().sync();
//                                            remoteCtx.close().awaitUninterruptibly();
//                                            ctx.writeAndFlush(receivedServerData);

//                                            group.shutdownGracefully();
    }
}
