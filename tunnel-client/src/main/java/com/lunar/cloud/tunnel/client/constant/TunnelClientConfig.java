package com.lunar.cloud.tunnel.client.constant;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "tunnel.client")
@Configuration
@Data
@NoArgsConstructor
public class TunnelClientConfig {
    /**
     * 真实服务端口
     */
    private Integer realPort = 28088;

    /**
     * 服务端口
     */
    private Integer serverPort = 16001;

    /**
     * 服务IP
     */
    private String serverIp = "127.0.0.1";

    /**
     * 服务IP
     */
    private String realServerIp = "127.0.0.2";
}
