package com.lunar.cloud.tunnel.client.constant;

import lombok.Data;
import lombok.NoArgsConstructor;
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
     * 服务端口，用于链接服务端完成注册
     */
    private Integer serverPort = 16001;

    /**
     * 访客服务端口，后期考虑修改为FRPC类似的，客户端期望服务端使用的端口，当服务器无法使用该端口时，会使用其他端口或者报错拒绝链接
     */
//    private Integer portalPort = 16002;
    private Integer portalPort = 29922;

    /**
     * 客户端注册token
     */
    private String token;
    /**
     * 代理服务IP
     */
    private String serverIp = "127.0.0.1";

    /**
     * 真实目标服务IP
     */
    private String realServerIp = "127.0.0.3";


    private Boolean enableTls = false;

    private String sslPrivateKeyPassword;

    private String sslPrivateKeyUrl;
}
