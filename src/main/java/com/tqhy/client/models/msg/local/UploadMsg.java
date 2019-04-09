package com.tqhy.client.models.msg.local;

import com.tqhy.client.models.msg.BaseMsg;
import lombok.*;

import java.io.File;

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

    @NonNull
    private String projectId;

    @NonNull
    private String projectName;

    private String token;

    private String batchNumber;
}
