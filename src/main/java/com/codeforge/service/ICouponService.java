package com.codeforge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codeforge.entry.Coupon;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 */
public interface ICouponService extends IService<Coupon> {
    void asyncGenerateCode(Coupon coupon);
}
