package com.tqhy.client.models.msg.local;

import com.tqhy.client.models.msg.BaseMsg;
import lombok.*;

import java.util.Map;

/**
 * @author Yiheng
 * @create 6/4/2019
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor(staticName = "of")
public class DownloadMsg extends BaseMsg {
    /**
     * 下载请求的接口名
     */
    @NonNull
    private String apiName;

    /**
     * 下载请求参数
     */
    @NonNull
    private Map<String, String> requestParamMap;
}
