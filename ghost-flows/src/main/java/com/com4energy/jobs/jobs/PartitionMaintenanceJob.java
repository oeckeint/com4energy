package com.com4energy.jobs.jobs;

import com.com4energy.jobs.service.PartitionMaintenanceService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PartitionMaintenanceJob implements Job {

    private final PartitionMaintenanceService partitionMaintenanceService;

    public PartitionMaintenanceJob(PartitionMaintenanceService partitionMaintenanceService) {
        this.partitionMaintenanceService = partitionMaintenanceService;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        log.info("Ejecutando mantenimiento de particiones anuales de medidas");
        partitionMaintenanceService.ensureUpcomingPartitions();
    }
}
