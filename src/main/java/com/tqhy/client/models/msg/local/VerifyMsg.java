package com.tqhy.client.models.msg.local;

import com.tqhy.client.models.msg.BaseMsg;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Yiheng
 * @create 3/21/2019
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
public class VerifyMsg extends BaseMsg {

    /**
     * AIC设备ip地址
     */
    private String serverIP;

    /**
     * 客户端序列号
     */
    private String serializableNum;

    /**
     * 激活码
     */
    private String activationCode;
}
