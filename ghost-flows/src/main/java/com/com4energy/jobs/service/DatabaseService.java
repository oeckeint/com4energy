package com.com4energy.jobs.service;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DatabaseService {

    @Value("${mysql.host}")
    private String host;

    @Value("${mysql.database}")
    private String database;

    @Value("${mysql.username}")
    private String username;

    @Value("${mysql.backup.path}")
    private String backupPath;

    public void backup() throws JobExecutionException {
        // Ensure the backup directory exists
        File backupDir = new File(backupPath);
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            log.error("Failed to create backup directory: {}", backupPath);
            throw new JobExecutionException("Failed to create backup directory: " + backupPath);
        }

        // Get current date and time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand());
        processBuilder.redirectOutput(new File(backupPath + "/backup_" + now.format(formatter) + ".sql"));
        processBuilder.redirectError(new File(backupPath + "/backup_error" + ".log"));

        try {
            Process process = processBuilder.start();
            int processComplete = process.waitFor();

            if (processComplete == 0) {
                log.info("Backup completed successfully");
            } else {
                log.error("Backup failed");
                try (BufferedReader br = new BufferedReader(new FileReader(backupPath + "/backup_error.log"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        log.error(line);
                    }
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroy();
                log.error("Backup process timed out.");
            }

        } catch (IOException e) {
            throw new JobExecutionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException(e);
        }
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add("mysqldump");
        command.add("-h");
        command.add(host);
        command.add("-P");
        command.add("3306");
        command.add("-u");
        command.add(username);
        command.add(database);
        return command;
    }
}