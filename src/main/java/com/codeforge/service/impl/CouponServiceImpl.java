package com.codeforge.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codeforge.util.CodeUtil;
import com.codeforge.domain.po.Coupon;
import com.codeforge.service.ICouponService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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
        // 4.写入Redis ZSet，记录该券对应的最大序列号，用于无库校验
        try {
            redisTemplate.opsForZSet().add(COUPON_RANGE_KEY, coupon.getId().toString(), maxSerialNum);
        }catch (Exception e){
            log.error("写入ZSet失败，券ID:{}", coupon.getId(), e);
        }
    }


    @Override
    public boolean saveBatch(Collection<Coupon> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<Coupon> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean updateBatchById(Collection<Coupon> entityList, int batchSize) {
        return false;
    }

    @Override
    public boolean saveOrUpdate(Coupon entity) {
        return false;
    }

    @Override
    public Coupon getOne(Wrapper<Coupon> queryWrapper, boolean throwEx) {
        return null;
    }

    @Override
    public Map<String, Object> getMap(Wrapper<Coupon> queryWrapper) {
        return null;
    }

    @Override
    public <V> V getObj(Wrapper<Coupon> queryWrapper, Function<? super Object, V> mapper) {
        return null;
    }

    @Override
    public BaseMapper<Coupon> getBaseMapper() {
        return null;
    }

    @Override
    public Class<Coupon> getEntityClass() {
        return null;
    }
}
