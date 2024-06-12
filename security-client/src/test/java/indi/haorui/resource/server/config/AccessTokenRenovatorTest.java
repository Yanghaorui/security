package indi.haorui.resource.server.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Yang Hao.rui on 2024/6/4
 */
@Slf4j
class AccessTokenRenovatorTest {


    @Test
    public void auto_new_job() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        AccessTokenRenovator.register("e",
                s -> new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token" + atomicInteger.getAndAdd(1),
                        Instant.now(),
                        Instant.now().plusSeconds(4) // 4秒过期 意味着第3秒会刷新token
                )
        );

        for (; ;Thread.sleep(1_000)) {
            log.info(Instant.now() + " auto_new_job " + Objects.requireNonNull(AccessTokenRenovator.get("e")).getTokenValue());
            if ("token3".equals(Objects.requireNonNull(AccessTokenRenovator.get("e")).getTokenValue())) {
                break;
            }
        }
    }

    @Test
    public void auto_refresh_job() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        AtomicInteger plusExpiredSecond = new AtomicInteger(4);
        AccessTokenRenovator.register("d",
                s -> new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token" + atomicInteger.getAndAdd(1),
                        Instant.now(),
                        // 第一个token 4秒过期 3秒的时候会刷新第二个token，第二个8秒过期 着第6秒会刷新token 以此类推
                        Instant.now().plusSeconds(plusExpiredSecond.getAndAdd(4))
                )
        );

        for (; ;Thread.sleep(1_000)) {
            log.info(Instant.now() + " auto_refresh_job " + Objects.requireNonNull(AccessTokenRenovator.get("d")).getTokenValue());
            if ("token3".equals(Objects.requireNonNull(AccessTokenRenovator.get("d")).getTokenValue())) {
                break;
            }
        }
    }


    @Test
    public void jobs_in_quarantine() throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        AccessTokenRenovator.register("b",
                s -> new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token" + atomicInteger.getAndAdd(1),
                        Instant.now(),
                        Instant.now().plusSeconds(4) // 4秒过期 意味着第3秒会刷新token
                )
        );

        AccessTokenRenovator.register("c",
                s -> new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "token" + atomicBoolean.getAndSet(!atomicBoolean.get()),
                        Instant.now(),
                        Instant.now().plusSeconds(8) // 8秒过期 意味着第6秒会刷新token
                )
        );

        for (; ;Thread.sleep(1_000)) {
            log.info("altered=====>" + Instant.now() + " " + Objects.requireNonNull(AccessTokenRenovator.get("c")).getTokenValue() + "   ");
            log.info("unaltered--->" + Instant.now() + " " + Objects.requireNonNull(AccessTokenRenovator.get("b")).getTokenValue());
            if ("token3".equalsIgnoreCase(Objects.requireNonNull(AccessTokenRenovator.get("b")).getTokenValue())) {
                break;
            }
        }
    }

    // 高并发同样的registrationId同时注册，其中一个会创建job，另外的会被阻塞，后续会拿到前一个job缓存的token
    @Test
    public void job_lock() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutorService executors = Executors.newCachedThreadPool();

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            executors.execute(()->{
                try {
                    countDownLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long currentTimeMillis = System.currentTimeMillis();
                log.info(String.valueOf(currentTimeMillis));
                AccessTokenRenovator.register("a",
                        s -> new OAuth2AccessToken(
                                OAuth2AccessToken.TokenType.BEARER,
                                "token" + finalI,
                                Instant.now(),
                                Instant.now().plusSeconds(4)
                        )
                );
            });
        }
        countDownLatch.countDown();
        Set<String> token = new HashSet<>();
        for(int i = 0;i<10;i++,Thread.sleep(1000)){
            if (Objects.nonNull(AccessTokenRenovator.get("a"))){
                String tokenValue = Objects.requireNonNull(AccessTokenRenovator.get("a")).getTokenValue();
                log.info(tokenValue);
                token.add(tokenValue);
            }
        }
        log.info("token size:{}",token.size()); //1个
    }


}