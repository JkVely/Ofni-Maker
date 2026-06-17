package com.ofni.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        return new ThreadPoolTaskExecutorBuilder()
            .corePoolSize(2)
            .maxPoolSize(4)
            .queueCapacity(10)
            .threadNamePrefix("ofni-async-")
            .build();
    }
}
