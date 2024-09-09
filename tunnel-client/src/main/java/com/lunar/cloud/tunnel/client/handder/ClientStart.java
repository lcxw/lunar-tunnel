package com.lunar.cloud.tunnel.client.handder;

import com.lunar.cloud.tunnel.client.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientStart {
    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            if (null != args && args.getNonOptionArgs().size() == 3) {
                int realPort = Integer.parseInt(args.getNonOptionArgs().get(2));
                int serverPort = Integer.parseInt(args.getNonOptionArgs().get(1));
                String serverIp = args.getSourceArgs()[0];
                Constant.serverIp = serverIp;
                Constant.serverPort = serverPort;
                Constant.realPort = realPort;
                // 连接代理服务
                ProxySocket.connectProxyServer();
            }
        };
    }

}