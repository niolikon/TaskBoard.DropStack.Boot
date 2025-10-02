package com.niolikon.taskboard.dropstack.documents.controller.scenarios;

import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultipleDocumentsWithPaginationScenario {

    public static final String USER_UUID = "user-2222";
    public static final int TOTAL = 23;

    public static Collection<?> getDataset() {
        List<DocumentEntity> docs = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            docs.add(DocumentEntity.builder()
                    .id(String.format("65%022d", i))
                    .objectKey("tenantA/2025/09/doc-%02d.pdf".formatted(i))
                    .bucket("taskboard-dropstack-docs")
                    .etag("etag-%02d".formatted(i))
                    .title("Doc %02d".formatted(i))
                    .mimeType("application/pdf")
                    .size(1000L + i)
                    .tags(List.of("paginable"))
                    .createdAt(Instant.parse("2025-09-01T10:00:00Z"))
                    .updatedAt(Instant.parse("2025-09-01T10:00:00Z"))
                    .ownerUid(USER_UUID)
                    .version(0L)
                    .categoryCode(null)
                    .checkedInAt(null)
                    .build());
        }
        return docs;
    }
}
