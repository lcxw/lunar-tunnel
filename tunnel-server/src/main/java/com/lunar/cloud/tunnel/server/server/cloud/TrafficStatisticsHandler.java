package com.lunar.cloud.tunnel.server.server.cloud;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrafficStatisticsHandler extends ChannelTrafficShapingHandler {

    public TrafficStatisticsHandler(long checkInterval) {
        super(checkInterval);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        log.trace("Channel active: " + ctx.channel().id());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.trace("Channel inactive: " + ctx.channel().id());
    }

    @Override
    protected void doAccounting(TrafficCounter counter) {
        super.doAccounting(counter);
        log.trace("Bytes read: " + counter.cumulativeReadBytes());
        log.trace("Bytes written: " + counter.cumulativeWrittenBytes());
    }
}