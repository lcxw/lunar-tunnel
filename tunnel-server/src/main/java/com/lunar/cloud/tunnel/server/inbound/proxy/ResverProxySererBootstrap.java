package com.lunar.cloud.tunnel.server.inbound.proxy;

import com.lunar.cloud.tunnel.core.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ResverProxySererBootstrap {
    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            if (null != args && args.getSourceArgs().length == 2) {
                int visitorPort = Integer.parseInt(args.getNonOptionArgs().getLast());
                int serverPort = Integer.parseInt(args.getNonOptionArgs().getFirst());
                Constant.visitorPort = visitorPort;
                Constant.serverPort = serverPort;
                // 启动访客服务端，用于接收访客请求
                VisitorSocket.startServer();
                // 启动代理服务端，用于接收客户端请求
                ServerSocket.startServer();
            }

        };
    }
}
