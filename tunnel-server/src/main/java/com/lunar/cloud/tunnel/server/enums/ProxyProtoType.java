package com.lunar.cloud.tunnel.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wang
 */

@AllArgsConstructor
@Getter
public enum ProxyProtoType {
    DIRECT(0, "direct", "直连模式"),
    SOCKS5(0, "socks5", "socket5协议模式"),
    TROJAN(0, "Trojan", "trojan协议模式");
    private final int code;
    private final String key;
    private final String name;
}
