package com.tqhy.client.models.msg.server;

import com.tqhy.client.models.msg.BaseMsg;

/**
 * @author Yiheng
 * @create 3/18/2019
 * @since 1.0.0
 */
public class ClientMsg<T> extends BaseMsg {
    public T bean;
}
