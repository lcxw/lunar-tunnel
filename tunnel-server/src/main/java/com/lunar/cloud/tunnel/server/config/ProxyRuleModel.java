package com.lunar.cloud.tunnel.server.config;

import com.lunar.cloud.tunnel.server.enums.ProxyModel;
import lombok.*;

/**
 * @author wang
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProxyRuleModel {
    ProxyModel proxyModel;
    private String host;
    private String port;
    private Boolean enabled;
}
