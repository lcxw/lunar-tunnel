package com.lunar.cloud.tunnel.server.config;

import com.lunar.cloud.tunnel.server.enums.ProxyModel;
import inet.ipaddr.HostName;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author kdyzm
 * @date 2021/4/25
 */
@Component
@Slf4j
@Data
public class ConfigContext {


    private final ServerConfig serverConfig;


    public ProxyModel getProxyMode(String dstAddr) {
        IPAddressString distAddressString = new IPAddressString(dstAddr);
        IPAddress distAddress = distAddressString.getAddress();
        ProxyModel proxyModel = null;
        for (ProxyRuleModel pacRule : serverConfig.getPacRuleList()) {
            String host = pacRule.getHost();
            HostName hostName = new HostName(host);
            try {
                if (hostName.isAddress()) {
                    IPAddressString ipAddressString = new IPAddressString(host);
                    IPAddress address = ipAddressString.getAddress();
                    if (address != null) {
                        if (address.contains(distAddress)) {
                            proxyModel= pacRule.getProxyModel();
                        }
                    }
                } else  {
                    if (dstAddr.endsWith(host)) {
                        proxyModel= pacRule.getProxyModel();
                    }
                }

            } catch (Exception e) {
                log.warn("skip pacRule that can not matched with illegal address");
            }
        }
        //如果不在pac名单中，默认行为取决于代理模式
        return proxyModel==null?getServerConfig().getProxyModel():proxyModel;
    }
}
