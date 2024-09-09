package com.lunar.cloud.tunnel.server.config;

import com.lunar.cloud.tunnel.server.enums.ProxyProtoType;
import lombok.*;

/**
 * @author wang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Socket5ServerConfig {
    private String host;
    private int port;
    private ProxyProtoType proxyProtoType;
    private Boolean authenticationEnabled = false;
    private String username = null;
    private String password = null;
}
