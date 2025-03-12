package com.lunar.cloud.tunnel.core.constant;


import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {
    // todo 这里只有一个客户端与代理服务器的连接，需要修改调整为支持多个客户端链接，大概是一个map，需要根据客户端注册到服务端的参数，区分不同的客户端链接，修改协议以支持注册时候提供客户端参数等，后期考虑客户端密码
    /** 客户端服务channel */
    public static Channel clientChannel = null;

    public static Map<Integer,Channel> clientChannelMap = new ConcurrentHashMap<>();

    /** 绑定channel_id */
    public static final AttributeKey<String> VID = AttributeKey.newInstance("vid");

    /**
     * 访客，客户服务channel，存储内网客户端到代理服务器的连接
     * */
    public static Map<String, Channel> vcc = new ConcurrentHashMap<>();

    /**
     * 访客，访客服务channel ，存储访客服务到代理服务器的连接
     * */
    public static Map<String, Channel> vvc = new ConcurrentHashMap<>();



    /**
     * 清除连接
     *
     * @param vid 访客ID
     */
    public static void clearVccVvc(String vid) {
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        Channel clientChannel = vcc.get(vid);
        if (null != clientChannel) {
            clientChannel.attr(VID).set(null);
            vcc.remove(vid);
        }
        Channel visitorChannel = vvc.get(vid);
        if (null != visitorChannel) {
            visitorChannel.attr(VID).set(null);
            vvc.remove(vid);
        }
    }

    /**
     * 清除关闭连接
     *
     * @param vid 访客ID
     */
    public static void clearVccVvcAndClose(String vid) {
        if (StringUtil.isNullOrEmpty(vid)) {
            return;
        }
        Channel clientChannel = vcc.get(vid);
        if (null != clientChannel) {
            clientChannel.attr(VID).set(null);
            vcc.remove(vid);
            clientChannel.close();
        }
        Channel visitorChannel = vvc.get(vid);
        if (null != visitorChannel) {
            visitorChannel.attr(VID).set(null);
            vvc.remove(vid);
            visitorChannel.close();
        }
    }
}