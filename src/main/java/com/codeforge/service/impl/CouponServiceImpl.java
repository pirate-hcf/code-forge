package com.codeforge.service.impl;


import com.codeforge.util.CodeUtil;
import com.codeforge.entry.Coupon;
import com.codeforge.service.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@Service
@Slf4j
public class CouponServiceImpl implements ICouponService {
    private final static String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    private final static String COUPON_RANGE_KEY = "coupon:code:range";
    private final StringRedisTemplate redisTemplate;
    private final BoundValueOperations<String, String> serialOps;

    public CouponServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.serialOps = redisTemplate.boundValueOps(COUPON_CODE_SERIAL_KEY);
    }
    @Override
    @Async("generateExchangeCodeExecutor")
    public void asyncGenerateCode(Coupon coupon) {
        // 发放数量
        Integer totalNum = coupon.getTotalNum();
        // 1.获取Redis自增序列号
        Long maxSerialNum = redisTemplate.opsForValue().increment(COUPON_CODE_SERIAL_KEY, totalNum);
        if (maxSerialNum == null) {
            log.error("获取序列号失败，券ID:{}", coupon.getId());
            return;
        }
        long startSerialNum = maxSerialNum - totalNum + 1;
        for (long serialNum = startSerialNum; serialNum <= maxSerialNum; serialNum++) {
            // 2.生成兑换码
            String code = CodeUtil.generateCode(serialNum, coupon.getId());
            log.debug("生成兑换码：{}，序列号：{}，券ID:{}", code, serialNum, coupon.getId());
        }
        log.info("券ID:{}, 发放量:{}, 序列号段:[{}, {}]", coupon.getId(), totalNum, startSerialNum, maxSerialNum);
        // 4.写入Redis ZSet，记录该券对应的最大序列号，用于无库校验
        try {
            redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), maxSerialNum);
        }catch (Exception e){
            log.error("写入ZSet失败，券ID:{}", coupon.getId(), e);
        }
    }


}
