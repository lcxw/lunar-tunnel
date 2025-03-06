package com.lunar.cloud.tunnel.server.server.cloud;

import com.lunar.cloud.tunnel.core.constant.Constant;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class ReverseTunnelDataTransforHandler extends ChannelDuplexHandler {
    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        super.read(ctx);

    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
    }
}
