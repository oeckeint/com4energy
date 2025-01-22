package com.com4energy.jobs.config;

import com.com4energy.jobs.jobs.BackupDatabaseJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail backupJobDetail() {
        return JobBuilder.newJob(BackupDatabaseJob.class)
                .withIdentity("backupDatabaseJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger trigger(JobDetail jobDetail){
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity("databaseBackupTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?")) // Ejecutar cada día a las 2 AM
                .build();
    }


}
