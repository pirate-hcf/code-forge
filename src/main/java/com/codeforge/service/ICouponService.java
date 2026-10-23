package com.codeforge.service;

import com.codeforge.entry.Coupon;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 */
public interface ICouponService {
    void asyncGenerateCode(Coupon coupon);
}
