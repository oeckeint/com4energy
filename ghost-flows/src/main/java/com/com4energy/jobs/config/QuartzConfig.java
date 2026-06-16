package com.com4energy.jobs.config;

import com.com4energy.jobs.jobs.BackupDatabaseJob;
import com.com4energy.jobs.jobs.PartitionMaintenanceJob;
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

    // Param nombrado 'backupJobDetail' para resolver por nombre (hay >1 bean JobDetail).
    @Bean
    public Trigger trigger(JobDetail backupJobDetail){
        return TriggerBuilder.newTrigger()
                .forJob(backupJobDetail)
                .withIdentity("databaseBackupTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
                .build();
    }

    @Bean
    public JobDetail partitionMaintenanceJobDetail() {
        return JobBuilder.newJob(PartitionMaintenanceJob.class)
                .withIdentity("partitionMaintenanceJob")
                .storeDurably()
                .build();
    }

    // Día 1 de cada mes a las 02:00: idempotente, asegura con holgura la partición del año entrante.
    @Bean
    public Trigger partitionMaintenanceTrigger(JobDetail partitionMaintenanceJobDetail){
        return TriggerBuilder.newTrigger()
                .forJob(partitionMaintenanceJobDetail)
                .withIdentity("partitionMaintenanceTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 1 * ?"))
                .build();
    }

}
