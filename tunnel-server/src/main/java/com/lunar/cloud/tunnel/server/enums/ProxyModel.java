package com.lunar.cloud.tunnel.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Arrays;

/**
 * @author kdyzm
 * @date 2021/5/11
 */
@AllArgsConstructor
@Getter
@ToString
public enum ProxyModel {

    /**
     * 全局代理模式：全部请求都走代理
     */
    GLOBAL("global", "全局代理模式"),

    /**
     * 白名单代理模式：在名单中的走代理
     */
    PAC("pac", "白名单代理模式"),

    /**
     * 直连模式：全部请求直连，不走代理
     */
    DIRECT("direct", "直连模式"),

    REJECT("reject", "拒绝模板");


    private final String mode;

    private final String desc;


    public static ProxyModel get(String mode) {
        return Arrays.stream(ProxyModel.values()).filter(item -> item.getMode().equalsIgnoreCase(mode)).findFirst().orElse(null);
    }
}
