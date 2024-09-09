package com.lunar.cloud.tunnel.server.config;

import com.lunar.cloud.tunnel.server.enums.ProxyModel;
import com.lunar.cloud.tunnel.server.enums.ProxyProtoType;
import inet.ipaddr.HostName;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author kdyzm
 * @date 2021-04-24
 */
@Configuration
@Component
@ComponentScan(basePackages = {"server.proxy"})
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "server.proxy")
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class ServerConfig implements InitializingBean
{
    private Boolean mixedSocksAndHttp = true;
    private Integer socksPort = 12345;
    private Integer httpPort = socksPort;
    private String listen;
    private Boolean authenticationEnabled = true;
    private String username;
    private String password;
    private Socket5ServerConfig upstreamServerConfig;
    private ProxyProtoType localServerProto = ProxyProtoType.SOCKS5;
    private ProxyModel proxyModel = ProxyModel.PAC;
    private List<ProxyRuleModel> pacRuleList;


    @Override
    public void afterPropertiesSet() throws Exception {
        for (ProxyRuleModel pacRule : pacRuleList) {
            String host = pacRule.getHost();
            try {
                HostName hostName = new HostName(host);
                hostName.validate();
//                IPAddressString addressString = new IPAddressString(host);
//                addressString.validate();
            }catch (Exception e){
                log.error("error parsing IP address from config ",e);
                throw e;
            }
        }
    }
}
