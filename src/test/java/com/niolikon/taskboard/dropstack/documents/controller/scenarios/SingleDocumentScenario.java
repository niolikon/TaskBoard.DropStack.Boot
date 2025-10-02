package com.niolikon.taskboard.dropstack.documents.controller.scenarios;

import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public class SingleDocumentScenario {

    public static final String USER_UUID = "user-1111";
    public static final String DOC_ID = "656565656565656565656565";
    public static final String DOC_TITLE = "Documento Uno";
    public static final long DOC_VERSION = 0L;

    public static Collection<?> getDataset() {
        DocumentEntity doc = DocumentEntity.builder()
                .id(DOC_ID)
                .objectKey("tenantA/2025/09/doc-1.pdf")
                .bucket("taskboard-dropstack-docs")
                .etag("etag-1")
                .title(DOC_TITLE)
                .mimeType("application/pdf")
                .size(12345L)
                .tags(List.of("tag1","tag2"))
                .createdAt(Instant.parse("2025-09-01T10:00:00Z"))
                .updatedAt(Instant.parse("2025-09-01T10:00:00Z"))
                .ownerUid(USER_UUID)
                .version(DOC_VERSION)
                .categoryCode(null)
                .checkedInAt(null)
                .build();

        return List.of(doc);
    }
}

