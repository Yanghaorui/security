package indi.haorui.securityclient.config;

import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Created by Yang Hao.rui on 2024/2/21
 * <p>
 * Token刷新器， 将Token有效时长的3/4作为刷新时间
 */
@Slf4j
public class AccessTokenRenovator {

    private static final Scheduler SCHEDULER;
    
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

    private static final Map<String, OAuth2AccessToken> ACCESS_TOKENS = new ConcurrentHashMap<>();

    public static OAuth2AccessToken register(String registrationId, Function<String, OAuth2AccessToken> execute) {
        OAuth2AccessToken accessToken = execute.apply(registrationId);
        int interval = interval(accessToken);
        newJob(registrationId, interval, execute);
        ACCESS_TOKENS.put(registrationId, accessToken);
        return accessToken;
    }

    public static String get(String registrationId) {
        OAuth2AccessToken accessToken = ACCESS_TOKENS.get(registrationId);
        if (Objects.nonNull(accessToken) && Objects.nonNull(accessToken.getExpiresAt())
                && accessToken.getExpiresAt().isAfter(Instant.now())) {
            return accessToken.getTokenValue();
        }
        return null;
    }

    /**
     * 创建一个新的任务
     *
     */
    private static void newJob(String registrationId, int interval, Function<String, OAuth2AccessToken> function) {
        SimpleScheduleBuilder rule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(interval)
                .repeatForever();
        JobDataMap jobDataMap = new JobDataMap(
                Map.of("registrationId", registrationId, "interval", interval, "function", function)
        );
        JobKey jobKey = jobKey(registrationId);
        JobDetail build = JobBuilder.newJob(Looper.class).usingJobData(jobDataMap).withIdentity(jobKey).build();
        SimpleTrigger simpleTrigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger@" + registrationId)
                .withSchedule(rule)
                .startAt(DateBuilder.futureDate(interval, DateBuilder.IntervalUnit.SECOND))
                .build();
        try {
            /*
             * 如果任务已经存在，删除任务
             */
            if (SCHEDULER.checkExists(jobKey)) {
                SCHEDULER.deleteJob(jobKey);
            }
            SCHEDULER.scheduleJob(build, simpleTrigger);
        } catch (SchedulerException e){
            log.error("Failed to create a new token job", e);
        }
    }

    private static JobKey jobKey(String identity) {
        return JobKey.jobKey(identity);
    }


    /**
     * 根据token的过期时间计算刷新时间  过期时间的四分之三
     * 默认 10 分钟
     * @return token 刷新时间 单位: s
     */
    private static int interval(OAuth2AccessToken oAuth2AccessToken) {
        Instant issuedAt = oAuth2AccessToken.getIssuedAt();
        Instant expiresAt = oAuth2AccessToken.getExpiresAt();
        if (Objects.nonNull(expiresAt) && Objects.nonNull(issuedAt)) {
            long until = issuedAt.until(expiresAt, ChronoUnit.SECONDS);
            return (int) (until * 3 / 4);
        }
        return 10 * 60 * 60;
    }

    /**
     * 更新OAuth2AccessToken, 如果token的过期时长发生变化，根据刷新时间重新创建任务
     */
    public static class Looper implements Job {

        /**
         * @param context 任务执行上下文
         */
        @Override
        @SuppressWarnings("unchecked")
        public void execute(JobExecutionContext context) {
            JobDataMap mergedJobDataMap = context.getMergedJobDataMap();
            int currentInterval = mergedJobDataMap.getInt("interval");
            String registrationId = mergedJobDataMap.getString("registrationId");
            Function<String, OAuth2AccessToken> function = (Function<String, OAuth2AccessToken>) mergedJobDataMap.get("function");
            OAuth2AccessToken accessToken = function.apply(registrationId);
            if (Objects.isNull(accessToken)) {
                log.error("Failed to renovate {} token", registrationId);
                return;
            }
            ACCESS_TOKENS.put(registrationId, accessToken);
            int newInterval = interval(accessToken);
            /*
             * 如果token的过期时长发生变化，根据刷新时间重新创建任务
             * 如果没有发生变化，不做任何操作
             */
            if (currentInterval == newInterval) {
                return;
            }
            newJob(registrationId, newInterval, function);
            log.info("Renovate {} interval from {} to {}", registrationId, currentInterval, newInterval);
        }
    }
}