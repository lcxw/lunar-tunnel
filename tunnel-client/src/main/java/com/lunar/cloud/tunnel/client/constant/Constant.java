package com.lunar.cloud.tunnel.client.constant;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;

public class Constant {
    /** 代理服务channel */
    public static Channel proxyChannel = null;

    /** 绑定访客id */
    public static final AttributeKey<String> VID = AttributeKey.newInstance("vid");

    /**
     *  访客，代理服务channel，存储客户端到代理服务器(公网)之间的链接
     *  */
    public static Map<String, Channel> vpc = new ConcurrentHashMap<>();

    /**
     *  访客，真实服务channel ，存储代理客户端到真实的内网服务之间的链接
     *  */
    public static Map<String, Channel> vrc = new ConcurrentHashMap<>();


    /**
     * 清除连接
     *
     * @param vid 访客ID
     */
    public static void clearvpcvrc(String vid) {
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        Channel clientChannel = vpc.get(vid);
        if (null != clientChannel) {
            clientChannel.attr(VID).set(null);
            vpc.remove(vid);
        }
        Channel visitorChannel = vrc.get(vid);
        if (null != visitorChannel) {
            visitorChannel.attr(VID).set(null);
            vrc.remove(vid);
        }
    }

    /**
     * 清除关闭连接
     *
     * @param vid 访客ID
     */
    public static void clearvpcvrcAndClose(String vid) {
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        Channel clientChannel = vpc.get(vid);
        if (null != clientChannel) {
            clientChannel.attr(VID).set(null);
            vpc.remove(vid);
            clientChannel.close();
        }
        Channel visitorChannel = vrc.get(vid);
        if (null != visitorChannel) {
            visitorChannel.attr(VID).set(null);
            vrc.remove(vid);
            visitorChannel.close();
        }
    }
}