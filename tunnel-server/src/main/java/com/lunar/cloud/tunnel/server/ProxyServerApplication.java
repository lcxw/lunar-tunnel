package com.lunar.cloud.tunnel.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableDiscoveryClient
@Slf4j
@EnableScheduling // 开启定时任务功能
@EnableAsync
@EnableCaching
public class ProxyServerApplication {



    public static void main(String[] args) {
        SpringApplication.run(ProxyServerApplication.class, args);
//        todo see git@github.com:3813g00c/flycat.git
        log.info("> todo see git@github.com:3813g00c/flycat.git");
        log.info("> todo see git@github.com:kdyzm/trojan-client-netty.git");
        log.info("> todo see git@github.com:kongwu-/netty-proxy-server.git");
    }

}
