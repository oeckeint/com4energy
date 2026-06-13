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

    private static final int TIMEOUT_MINUTES = 10;

    public void backup() throws JobExecutionException {
        File backupDir = new File(backupPath);

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            throw new JobExecutionException("Failed to create backup directory: " + backupPath);
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        File outputFile = new File(backupDir, "backup_" + timestamp + ".sql");
        File errorFile = new File(backupDir, "backup_error_" + timestamp + ".log");

        ProcessBuilder pb = new ProcessBuilder(buildCommand());
        pb.redirectOutput(outputFile);
        pb.redirectError(errorFile);

        try {
            Process process = pb.start();

            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                throw new JobExecutionException("Backup process timed out");
            }

            int exitCode = process.exitValue();

            if (exitCode == 0) {
                log.info("Backup completed successfully: {}", outputFile.getAbsolutePath());
            } else {
                log.error("Backup failed with exit code {}", exitCode);
                logErrorFile(errorFile);
                throw new JobExecutionException("Backup failed, see log: " + errorFile.getAbsolutePath());
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Backup execution failed", e);
        }
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();

        command.add("mysqldump");
        command.add("--no-tablespaces");
        command.add("--single-transaction");
        command.add("--routines");
        command.add("--triggers");
        command.add("--events");
        command.add("--skip-lock-tables");
        command.add("--quick");
        command.add("--skip-add-locks");
        command.add("--skip-disable-keys");
        command.add("--skip-comments");
        command.add("--skip-set-charset");
        command.add("--default-character-set=utf8mb4");
        command.add("--set-gtid-purged=OFF");
        command.add("--column-statistics=0");

        command.add("-h");
        command.add(host);

        command.add("-P");
        command.add("3306");

        command.add("-u");
        command.add(username);

        command.add("--ignore-table=" + database + ".schema_table_statistics");
        command.add("--ignore-table=" + database + ".schema_table_statistics_with_buffer");

        command.add(database);

        return command;
    }

    private void logErrorFile(File errorFile) {
        if (!errorFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(errorFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                log.error(line);
            }
        } catch (IOException e) {
            log.error("Failed to read error log file", e);
        }
    }
}