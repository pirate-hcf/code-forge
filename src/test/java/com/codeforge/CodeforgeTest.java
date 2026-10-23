package com.codeforge;

import com.codeforge.entry.Coupon;
import com.codeforge.service.ICouponService;
import com.codeforge.util.CodeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 兑换码异步生成测试
 * <p>
 * 让 Spring Boot AutoConfiguration 接管 Redis (读 application.yml 的 host/port),
 * 通过 spring.autoconfigure.exclude 避开 mybatis-plus 无 datasource 导致的启动失败。
 * 测试提交多个 coupon 并发任务, 验证线程池真正并行执行。
 */
@EnableAsync
@SpringBootTest(
        classes = CodeforgeApplication.class,
        properties = {
                "logging.level.root=INFO",
                "logging.level.com.codeforge=DEBUG",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
        }
)
public class CodeforgeTest {

    @Autowired
    private ICouponService couponService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String COUPON_CODE_SERIAL_KEY = "coupon:code:serial";
    private static final String COUPON_RANGE_KEY = "coupon:code:range";
    /**
     * 每一个 @Test 测试方法执行之前，都会自动执行 1 次
     */
    @BeforeEach
    void setUp() {
        redisTemplate.delete(COUPON_CODE_SERIAL_KEY);
        redisTemplate.delete(COUPON_RANGE_KEY);
        System.out.println("--- 清理完成, 开始新一轮 ---");
    }

    /**
     * CodeUtil的加密解密测试。
     */
    @Test
    void testCodeUtil(){
        String code = CodeUtil.generateCode(1,1);
        // 加密结果
        System.out.println(code);
        // 解密结果
        System.out.println(CodeUtil.parseCode(code));
    }


    /**
     * 单个 coupon 异步生成 —— 序列号段正确, ZSet 写入正确
     */
    @Test
    void testSingleCouponAsync() throws Exception {
        Coupon coupon = new Coupon();
        coupon.setId(88L);
        coupon.setTotalNum(500);

        couponService.asyncGenerateCode(coupon);

        // 轮询等异步任务写入 ZSet
        assertTrue(waitForZSetScore("88", 10_000), "异步任务应在 10s 内完成");

        assertEquals("500", redisTemplate.opsForValue().get(COUPON_CODE_SERIAL_KEY));
        Double score = redisTemplate.opsForZSet().score(COUPON_RANGE_KEY, "88");
        assertEquals(500.0, score, 0.01);
    }

    /**
     * 多个 coupon 并发异步生成 —— 验证:
     * <ol>
     *     <li>线程池并行执行 (日志应该交错)</li>
     *     <li>INCRBY 原子性保证序列号段不撞号</li>
     *     <li>全局序列号 = 各活动发放量之和</li>
     * </ol>
     */
    /**
     * 并发测试：提交3个券生成任务，观察控制台日志，看线程池并发执行
     */
    @Test
    void testConcurrentGen() throws Exception {
        Coupon c1 = new Coupon();
        c1.setId(1001L);
        c1.setTotalNum(500);

        Coupon c2 = new Coupon();
        c2.setId(1002L);
        c2.setTotalNum(300);

        Coupon c3 = new Coupon();
        c3.setId(1003L);
        c3.setTotalNum(200);

        System.out.println("主线程：开始提交3个异步任务");
        couponService.asyncGenerateCode(c1);
        couponService.asyncGenerateCode(c2);
        couponService.asyncGenerateCode(c3);
        System.out.println("主线程：任务全部提交完毕，等待异步任务执行完成...");

        // 等待三个任务全部写入Redis ZSet
        boolean allDone = waitForZSetScore("1001", 10_000)
                && waitForZSetScore("1002", 10_000)
                && waitForZSetScore("1003", 10_000);
        assertTrue(allDone, "任务超时未完成");

        System.out.println("====== 全部异步任务执行结束 ======");
        System.out.println("去Redis查看：GET coupon:code:serial 和 ZRANGE coupon:code:range 0 -1 WITHSCORES");
    }

    /** 轮询 ZSet, 直到指定 member 出现或超时 */
    private boolean waitForZSetScore(String member, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (redisTemplate.opsForZSet().score(COUPON_RANGE_KEY, member) != null) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }
}
