package com.lunar.cloud.tunnel.core.protocol;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 请求类型
 */
@RequiredArgsConstructor
@Getter
@ToString
public enum MessageType {

    /**
     * 认证请求
     */
    AUTH_REQUEST(0X03),

    /**
     * 正常请求
     */
    TYPE_TRANSFER(0X02),
    TYPE_HEARTBEAT(0X00),
    TYPE_CONNECT(0x01),
    TYPE_DISCONNECT(0X09);
    private final int value;
    private static final Map<Integer, MessageType> typeMap = new HashMap<>();
    static {
        Arrays.stream(values()).forEach(e -> typeMap.put(e.value, e));
    }

    public static MessageType fromCode(int code) {
        return typeMap.getOrDefault(code, TYPE_DISCONNECT);
    }
}
