package com.lunar.cloud.tunnel.server.inbound;

import com.lunar.cloud.tunnel.server.config.ConfigContext;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author kdyzm
 * @date 2021/4/25
 */
@Slf4j
@AllArgsConstructor
public class Socks5PasswordAuthRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    private final ConfigContext configContext;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        String username = msg.username();
        String password = msg.password();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            log.error("you should provider the username and password in socks5 proxy model");
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            //发送鉴权失败消息，完成后关闭channel
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);

        } else {
            //认证成功
            String confinedUsername = configContext.getServerConfig().getUsername();
            String confinedPassword = configContext.getServerConfig().getPassword();
            if (username.equals(confinedUsername) && password.trim().equals(confinedPassword)) {
                log.info("socks user and password match");
                Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
                ctx.writeAndFlush(passwordAuthResponse);
                ctx.pipeline().remove(this);
                ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
            } else {
                log.error("socks5 authorization failed with username:{} and password:{}", username, password);
                Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
                //发送鉴权失败消息，完成后关闭channel
                ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
                log.debug("send socks auth failed msg and close socket");
            }

        }

    }
}
