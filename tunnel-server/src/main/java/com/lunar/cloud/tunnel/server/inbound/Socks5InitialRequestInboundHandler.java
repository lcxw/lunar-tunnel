package com.lunar.cloud.tunnel.server.inbound;

import com.lunar.cloud.tunnel.server.config.ConfigContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author kdyzm
 * @date 2021-04-23
 */
@Slf4j
@AllArgsConstructor
public class Socks5InitialRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private final ConfigContext configProperties;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        log.debug("初始化socks5链接");
        boolean failure = msg.decoderResult().isFailure();
        if (failure) {
            log.error("初始化socks5失败，请检查是否是socks5协议:{}", msg.decoderResult());
//            ReferenceCountUtil.retain(msg);
//            ctx.writeAndFlush(Unpooled.wrappedBuffer("protocol version illegal!".getBytes()));
            ctx.fireChannelRead(msg);
            return;
        }
        if (configProperties.getServerConfig().getAuthenticationEnabled()) {
            log.debug("socket5 认证开启，需要密码进行认证");
            Socks5InitialResponse socks5InitialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
            ctx.writeAndFlush(socks5InitialResponse);
        } else {
            log.debug("socket5 authorization with no password");
            Socks5InitialResponse socks5InitialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
            ctx.writeAndFlush(socks5InitialResponse);
        }
        log.info("socks5 链接初始化成功,移除初始化handler:{{},{}}", this.getClass().getName(), Socks5InitialRequestDecoder.class.getName());
        ctx.pipeline().remove(this);
        ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
    }
}
