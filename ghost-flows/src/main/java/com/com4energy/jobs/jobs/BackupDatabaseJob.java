package com.com4energy.jobs.jobs;

import com.com4energy.jobs.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BackupDatabaseJob implements Job {

    private final DatabaseService databaseService;

    public BackupDatabaseJob(DatabaseService databaseService){
        this.databaseService = databaseService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        log.info("Backup started...");
        databaseService.backup();
    }
}
