package com.tqhy.client.models.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Yiheng
 * @create 6/4/2019
 * @since 1.0.0
 */
@Getter
@Setter
@ToString
public class DownloadInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;

    private String imgUrlString;
}
