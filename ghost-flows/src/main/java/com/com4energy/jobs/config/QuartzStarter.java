package com.com4energy.jobs.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuartzStarter {

    private final Scheduler scheduler;

    public QuartzStarter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void startScheduler() {
        try {
            scheduler.start();
            log.info("🔥 Quartz Scheduler iniciado manualmente");
        } catch (SchedulerException e) {
            throw new RuntimeException("Error iniciando Quartz", e);
        }
    }

}
