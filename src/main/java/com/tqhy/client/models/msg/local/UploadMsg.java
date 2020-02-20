package com.tqhy.client.models.msg.local;

import com.tqhy.client.models.msg.BaseMsg;
import lombok.*;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor(staticName = "with")
public class UploadMsg extends BaseMsg {

    public static final String UPLOAD_TYPE_CASE = "case";

    public static final String UPLOAD_TYPE_TEST = "test";
    /**
     * 上传类型
     */
    @NonNull
    private String uploadType;

    private String token;

    private String batchNumber;

    private String remarks;
}
