package com.com4energy.jobs.controller;

import com.com4energy.jobs.service.DatabaseService;
import org.quartz.JobExecutionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BackupController {

    private final DatabaseService databaseService;

    public BackupController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @GetMapping("/database/backup")
    public String backupDatabase() throws JobExecutionException {
        databaseService.backup();
        return "Done.";
    }

}