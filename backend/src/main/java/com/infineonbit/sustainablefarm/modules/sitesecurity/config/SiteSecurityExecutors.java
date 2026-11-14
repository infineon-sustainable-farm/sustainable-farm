package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded background executors of this module (the application's default async
 * executor has an unbounded queue). Public requests (email codes, password reset
 * requests) are dropped silently when the queue is full: their answer is generic
 * anyway. Account emails run the task on the caller instead, so none is lost.
 */
@Configuration
public class SiteSecurityExecutors {

    public static final String PUBLIC_REQUESTS = "siteSecurityPublicRequests";
    public static final String ACCOUNT_EMAILS = "siteSecurityAccountEmails";

    @Bean(name = PUBLIC_REQUESTS)
    public ThreadPoolTaskExecutor siteSecurityPublicRequests() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("farmsec-public-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = ACCOUNT_EMAILS)
    public ThreadPoolTaskExecutor siteSecurityAccountEmails() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("farmsec-mail-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
