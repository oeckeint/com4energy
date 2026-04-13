package com.com4energy.recordsapi.messaging.filerecord;

import com.com4energy.recordsapi.domain.entity.messaging.FileRecordEvent;
import com.com4energy.recordsapi.repository.FileRecordEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRecordConsumerTest {

    @Test
    void mapToEntityUsesOriginalFilenameFallbackAndPersistsMetadataAndRawPayload() throws Exception {
        FileRecordConsumer consumer = new FileRecordConsumer(
                new FileRecordRoutingProperties(),
                org.mockito.Mockito.mock(FileRecordEventRepository.class),
                new ObjectMapper()
        );

        Map<String, Object> payload = Map.of(
                "fileRecordId", 55,
                "eventType", "FILE_DEFECT_REPORT_CREATED",
                "originalFilename", "P1D_0021_0894_20240104.0",
                "status", "SUCCEEDED",
                "comment", "Se generó reporte.sge_defect.jsonl con 2 incidencia(s)",
                "lineReference", "/tmp/reporte.sge_defect.jsonl",
                "metadata", Map.of(
                        "phase", "validation",
                        "incidentCount", 2,
                        "defectFilePath", "/tmp/reporte.sge_defect.jsonl"
                )
        );

        Method method = FileRecordConsumer.class.getDeclaredMethod("mapToEntity", Map.class, String.class);
        method.setAccessible(true);
        FileRecordEvent entity = (FileRecordEvent) method.invoke(consumer, payload, "FILE_DEFECT_REPORT_CREATED");

        assertEquals("55", entity.getSourceId());
        assertEquals("P1D_0021_0894_20240104.0", entity.getFilename());
        assertEquals("SUCCEEDED", entity.getStatus());
        assertEquals("/tmp/reporte.sge_defect.jsonl", entity.getFailedLineReference());
        assertNotNull(entity.getMetadataJson());
        assertNotNull(entity.getRawPayload());
        assertTrue(entity.getMetadataJson().contains("\"phase\":\"validation\""));
        assertTrue(entity.getRawPayload().contains("\"originalFilename\":\"P1D_0021_0894_20240104.0\""));
    }
}

