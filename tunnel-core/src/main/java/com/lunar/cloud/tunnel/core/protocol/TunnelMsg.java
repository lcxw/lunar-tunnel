package com.lunar.cloud.tunnel.core.protocol;


import lombok.Data;

@Data
public class TunnelMsg {

    /** 心跳 */
    public static final byte TYPE_HEARTBEAT = 0X00;

    /** 连接成功 */
    public static final byte TYPE_CONNECT = 0X01;

    /** 数据传输 */
    public static final byte TYPE_TRANSFER = 0X02;

    /** 连接断开 */
    public static final byte TYPE_DISCONNECT = 0X09;

    /** 数据类型 */
    private byte type;

    /** 消息传输数据 */
    private byte[] data;


}