package com.lunar.cloud.tunnel.server.inbound;

import com.lunar.cloud.tunnel.server.config.ConfigContext;
import com.lunar.cloud.tunnel.server.inbound.http.HttpProxyInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MixinSelectHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ConfigContext configContext;

    private final EventLoopGroup clientWorkGroup;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {

            log.info("进入:{}", this.getClass().getName());
            final int readerIndex = msg.readerIndex();
            if (msg.writerIndex() == readerIndex) {
                return;
            }

            ChannelPipeline pipeline = ctx.pipeline();
            final byte versionVal = msg.getByte(readerIndex);
            log.info("try to resolve request type");
            SocksVersion version = SocksVersion.valueOf(versionVal);
            if (version.equals(SocksVersion.SOCKS4a) || version.equals(SocksVersion.SOCKS5)) {
                log.info("request is socks proxy version:{}", version);
                //socks proxy

//                pipeline.addLast(new SocksPortUnificationServerHandler(),
//                 SocksServerHandler.INSTANCE).remove(this);
                //socks5响应最后一个encode
                log.info("add :{} to pipeline：{}", Socks5ServerEncoder.DEFAULT.getClass().getName(), pipeline);
                pipeline.addLast(Socks5ServerEncoder.DEFAULT);

                //处理socks5初始化请求
                log.info("add :{}", Socks5InitialRequestDecoder.class.getName());
                pipeline.addLast(new Socks5InitialRequestDecoder());
                log.info("add :{}", Socks5InitialRequestInboundHandler.class.getName());
                pipeline.addLast(new Socks5InitialRequestInboundHandler(configContext));

                //处理认证请求
                if (Boolean.TRUE.equals(configContext.getServerConfig().getAuthenticationEnabled())) {
                    log.info("the socks authentication is enabled");
                    log.info("add :{}", Socks5PasswordAuthRequestDecoder.class.getClass().getName());
                    pipeline.addLast(new Socks5PasswordAuthRequestDecoder());
                    log.info("add :{}", Socks5PasswordAuthRequestInboundHandler.class.getClass().getName());
                    pipeline.addLast(new Socks5PasswordAuthRequestInboundHandler(configContext));
                } else {
                    log.info("the socks authentication is not enabled");
                    //处理connection请求
                    log.info("add:{}", Socks5CommandRequestDecoder.class.getName());
                    pipeline.addLast(new Socks5CommandRequestDecoder());
                    log.info("add:{}", Socks5CommandRequestInboundHandler.class.getName());
                    pipeline.addLast(new Socks5CommandRequestInboundHandler(clientWorkGroup, configContext));
                }


            } else {
                log.info("request is http proxy version:{}", version);
                //http/tunnel proxy
//                pipeline.addLast(new HttpServerHeadDecoder()).remove(this);
                pipeline.addLast("httpcode", new HttpServerCodec());
                pipeline.addLast("httpservice", new HttpProxyInboundHandler(configContext, clientWorkGroup));

            }
            msg.retain();
            ctx.fireChannelRead(msg);
//            ctx.flush();
//            ctx.pipeline().remove(this);
            log.info("完成请求类型解析，移除:{}", this.getClass().getName());
            ctx.pipeline().remove(MixinSelectHandler.class);
        } catch (Exception e) {
            log.error("error occurred while processing the request, the request can not be resoled", e);
            throw e;
        }
    }

}
