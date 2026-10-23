package com.codeforge.entry;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Coupon implements Serializable {

    private Long id;

    /**
     * 总数量，不超过5000
     */
    private Integer totalNum;
    /**
     * 兑换码
     */
    private String code;
}