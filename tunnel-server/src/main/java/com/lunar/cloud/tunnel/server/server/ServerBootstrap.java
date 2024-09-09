package com.lunar.cloud.tunnel.server.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerBootstrap {
    private final NettyServer nettyServer;

    @Bean
    public ApplicationRunner applicationRunner() {
        return (args -> {
            List<String> nonOptionArgs = args.getNonOptionArgs();
            log.info("Runner1[非选项参数]>>> " + nonOptionArgs);
            Set<String> optionNames = args.getOptionNames();
            for (String optionName : optionNames) {
                log.info("Runner1[选项参数]>>> name:" + optionName
                    + ";value:" + args.getOptionValues(optionName));
            }
            try {
                nettyServer.start();
                log.info("""
                    you can test server with:
                    raw socks,http tunnel ,socks5 or socks4,note that current only socks5 protocol are supported.
                    1. > curl --socks5 127.0.0.1:30808 -v -L http://172.17.17.68:38003/k8s/liveness
                    2. > curl -x http://127.0.0.1:30809 -v -L http://172.17.17.68:38003/k8s/liveness
                    3. > curl -x socks5://test:test@127.0.0.1:30808 http://172.17.17.68:38003/k8s/liveness
                       """);
            } catch (Exception e) {
                log.error("netty server 启动失败", e);
                throw e;
            }
        });
    }
}
