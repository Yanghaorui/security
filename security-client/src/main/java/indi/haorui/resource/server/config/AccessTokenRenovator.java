package indi.haorui.resource.server.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Created by Yang Hao.rui on 2024/5/29
 * <p>
 * Token刷新器， 将Token有效时长的3/4作为刷新时间
 */
@Slf4j
@Getter
public class AccessTokenRenovator {

    private static final Scheduler SCHEDULER;

    private static final Map<String, ReentrantLock> LOCK_MAP = new ConcurrentHashMap<>();

    /*
     * 通过静态代码块初始化调度器
     */
    static {
        try {
            SCHEDULER = StdSchedulerFactory.getDefaultScheduler();
            SCHEDULER.start();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Map<String, AccessTokenRenovator> RENOVATOR_MAP = new ConcurrentHashMap<>();


    private final Function<String, OAuth2AccessToken> execute;

    @Setter
    private OAuth2AccessToken accessToken;

    @Setter
    private int interval;

    private final String registrationId;

    private OAuth2AccessToken getAccessToken() {
        if (Objects.nonNull(accessToken) && Objects.nonNull(accessToken.getExpiresAt())
                && accessToken.getExpiresAt().isAfter(Instant.now())) {
            return accessToken;
        }
        return null;
    }


    private AccessTokenRenovator(String registrationId, Function<String, OAuth2AccessToken> execute) {
        this.registrationId = registrationId;
        this.execute = execute;
        this.accessToken = execute.apply(registrationId);
        if (Objects.isNull(accessToken)) {
            log.error("Failed to renovate {} token, retry later", registrationId);
        }
        this.interval = interval();
        newJob();
        RENOVATOR_MAP.put(registrationId, this);
    }
    /**
     * 注册到Schedule 并且返回一个token
     * <p>根据registrationId 加锁，防止并发请求时，重复创建任务
     * <p>对于同一个registrationId,只会创建一个任务
     * @param registrationId 注册id
     * @param execute 获取token的方法
     * @return OAuth2AccessToken
     */
    public static OAuth2AccessToken register(String registrationId, Function<String, OAuth2AccessToken> execute) {
        // 尝试从缓存中获取
        AccessTokenRenovator accessTokenRenovator = RENOVATOR_MAP.get(registrationId);
        if (Objects.nonNull(accessTokenRenovator) && Objects.nonNull(accessTokenRenovator.getAccessToken())) {
            return accessTokenRenovator.getAccessToken();
        }
        // 根据registrationId 加锁，防止并发请求时，重复创建任务
        ReentrantLock lock = LOCK_MAP.computeIfAbsent(registrationId, k -> new ReentrantLock());
        lock.lock();
        try {
            // 锁中再次尝试从缓存中获取
            accessTokenRenovator = RENOVATOR_MAP.get(registrationId);
            if (Objects.nonNull(accessTokenRenovator) && Objects.nonNull(accessTokenRenovator.getAccessToken())) {
                return accessTokenRenovator.getAccessToken();
            }
            // 创建一个新的任务，并返回token（在创建任务时会去获取token）
            accessTokenRenovator = new AccessTokenRenovator(registrationId, execute);
            return accessTokenRenovator.getAccessToken();
        } finally {
            lock.unlock();
        }
    }

    public static OAuth2AccessToken get(String registrationId) {
        AccessTokenRenovator accessTokenRenovator = RENOVATOR_MAP.get(registrationId);
        if (Objects.isNull(accessTokenRenovator)) {
            return null;
        }
        OAuth2AccessToken accessToken = accessTokenRenovator.getAccessToken();
        if (Objects.nonNull(accessToken) && Objects.nonNull(accessToken.getExpiresAt())
                && accessToken.getExpiresAt().isAfter(Instant.now())) {
            return accessToken;
        }
        return null;
    }

    /**
     * 创建一个新的任务
     */
    private void newJob() {
        SimpleScheduleBuilder rule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(interval)
                .repeatForever();
        JobKey jobKey = jobKey(registrationId);
        JobDetail build = JobBuilder.newJob(Looper.class).withIdentity(jobKey).build();
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger@" + registrationId)
                .withSchedule(rule)
                .startAt(DateBuilder.futureDate(interval, DateBuilder.IntervalUnit.SECOND))
                .build();
        try {
            /*
             * 如果任务已经存在，删除任务
             */
            deleteJob(registrationId);
            SCHEDULER.scheduleJob(build, simpleTrigger);
        } catch (SchedulerException e) {
            log.error("Failed to create a new token job", e);
        }
    }

    private static JobKey jobKey(String identity) {
        return JobKey.jobKey(identity);
    }


    /**
     * 根据token的过期时间计算刷新时间  过期时间的四分之三
     * 默认 5 分钟
     * <p> 如果token为null, 则返回 10秒钟
     *
     * @return token 刷新时间 单位: s
     */
    private int interval() {
        if (Objects.isNull(accessToken)) {
            return 10 * 60;
        }
        Instant issuedAt = accessToken.getIssuedAt();
        Instant expiresAt = accessToken.getExpiresAt();
        if (Objects.nonNull(expiresAt) && Objects.nonNull(issuedAt)) {
            long until = issuedAt.until(expiresAt, ChronoUnit.SECONDS);
            return (int) (until * 3 / 4);
        }
        return 5 * 60 * 60;
    }

    private static void deleteJob(String registrationId) {
        try {
            JobKey jobKey = jobKey(registrationId);
            if (SCHEDULER.checkExists(jobKey)) {
                SCHEDULER.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to delete job", e);
        }
    }

    /**
     * 更新OAuth2AccessToken, 如果token的过期时长发生变化，根据刷新时间重新创建任务
     */
    public static class Looper implements Job {

        /**
         * @param context 任务执行上下文
         */
        @Override
        public void execute(JobExecutionContext context) {
            String registrationId = context.getJobDetail().getKey().getName();
            AccessTokenRenovator accessTokenRenovator = RENOVATOR_MAP.get(registrationId);
            if(Objects.isNull(accessTokenRenovator)){
                deleteJob(registrationId);
            }
            // 获取当前token的刷新时间
            int currentInterval = accessTokenRenovator.interval();
            OAuth2AccessToken accessToken = accessTokenRenovator.getExecute().apply(registrationId);
            if (Objects.isNull(accessToken)) {
                log.error("Failed to renovate {} token", registrationId);
                return;
            }
            accessTokenRenovator.setAccessToken(accessToken);
            // 获取新的token的刷新时间
            int newInterval = accessTokenRenovator.interval();
            /*
             * 如果token的过期时长发生变化，根据刷新时间重新创建任务
             * 如果没有发生变化，不做任何操作
             */
            if (currentInterval == newInterval) {
                return;
            }
            accessTokenRenovator.setInterval(newInterval);
            accessTokenRenovator.newJob();
            log.info("Renovate {} interval from {} to {}", registrationId, currentInterval, newInterval);
        }
    }
}