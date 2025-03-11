package com.lunar.cloud.tunnel.core.protocol.handdler;

import com.lunar.cloud.tunnel.core.protocol.MessageType;
import com.lunar.cloud.tunnel.core.protocol.TunnelAuthMsg;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 客户端认证处理器
 */
@Slf4j
public class ClientAuthHandler extends ChannelInboundHandlerAdapter {
    // 标记客户端是否已经通过认证
    private boolean authenticated = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("新客户端链接");
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("客户端断开");
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TunnelAuthMsg) {
            TunnelAuthMsg authMsg = (TunnelAuthMsg) msg;
            // 进行认证处理
            if (authenticate(authMsg)) {
                log.info("客户端认证成功");
                authenticated = true;
                // 认证成功后，从ChannelPipeline中移除认证处理器
                ctx.pipeline().remove(this);
            } else {
                log.error("客户端认证失败");
                ctx.close();
                return;
            }
        } else {
            log.error("接收到非认证消息，关闭连接");
            ctx.close();
            return;
        }
        super.channelRead(ctx, msg);
    }

    /**
     * 认证方法，这里只是示例，你需要根据实际情况实现具体的认证逻辑
     *
     * @param msg 接收到的消息
     * @return 认证是否成功
     */
    private boolean authenticate(Object msg) {
        // 这里可以添加具体的认证逻辑，例如检查消息中的认证信息
        if (msg instanceof TunnelAuthMsg) {
            TunnelAuthMsg customMessage = (TunnelAuthMsg) msg;
            if (customMessage.getHeader().getType() == MessageType.AUTH_REQUEST) {
                String appId = customMessage.getAppId();
                String appSecret = customMessage.getAppSecret();
                if (StringUtils.hasText(appId) && StringUtils.hasText(appSecret)) {
                    authenticated = true;

                } else {
                    log.error("客户端认证失败");
                    authenticated = false;
                    return false;
                }


            } else {
                // 未认证时收到非认证消息，关闭连接
                return false;
            }
            return true; // 示例返回值，实际需要根据认证结果返回
        } else {
            return true; // 示例返回值，实际需要根据认证结果返回

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("客户端认证异常", cause);
        super.exceptionCaught(ctx, cause);
    }
}
