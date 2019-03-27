package com.tqhy.client.models;

import lombok.*;

import java.io.Serializable;

/**
 * @author Yiheng
 * @create 3/22/2019
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User  implements Serializable {

    private static final long serialVersionUID = 6855572509422134755L;
    private String userName;
    private String passWord;
}
