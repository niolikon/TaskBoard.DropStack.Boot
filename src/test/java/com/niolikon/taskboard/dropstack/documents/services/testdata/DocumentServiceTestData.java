package com.niolikon.taskboard.dropstack.documents.services.testdata;

import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateContentDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateMetadataDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentUpdateDto;
import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import com.niolikon.taskboard.dropstack.storage.model.ObjectStat;
import org.bson.types.ObjectId;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

public class DocumentServiceTestData {

    public static final String VALID_OWNER_UID = "user-abc";
    public static final String VALID_EXISTENT_DOC_ID = new ObjectId().toHexString();
    public static final String VALID_NON_EXISTENT_DOC_ID = new ObjectId().toHexString();

    public static final String DEFAULT_BUCKET_FOR_TESTS = "bucket-test";
    public static final String DOC_BUCKET = "docs-bucket";
    public static final String DOC_OBJECT_KEY = "obj-123";
    public static final String DOC_TITLE_NON_BLANK = "my-file.pdf";
    public static final String MIME_PDF = "application/pdf";
    public static final String DOCUMENT_CONTENT_DEFAULT_TYPE = "application/octet-stream";
    public static final String ETAG_VALUE = "etag-xyz";
    public static final long CONTENT_SIZE = 42L;
    public static final byte[] CONTENT_BYTES = "hello world".getBytes(StandardCharsets.UTF_8);

    public static final String VALID_DOC_ID = new ObjectId().toHexString();

    public static final Pageable pageable_firstPageSize10_fromClient = PageRequest.of(0, 10);

    // ---------- ObjectStat (mock) ----------
    public static final ObjectStat objectStat_withEtag_andContentType = ObjectStat.builder()
            .etag(ETAG_VALUE)
            .contentType(MIME_PDF)
            .size(CONTENT_SIZE)
            .build();

    // ---------- DTOs input (mock per content & metadata) ----------
    public static final DocumentCreateMetadataDto metadata_valid_fromClient = DocumentCreateMetadataDto.builder()
            .mimeType(MIME_PDF)
            .build();
    public static final DocumentCreateContentDto content_valid_fromClient = DocumentCreateContentDto.builder()
            .source(
                    new ByteArrayResource(CONTENT_BYTES) {
                        @Override public String getFilename() { return DOC_TITLE_NON_BLANK; }
                    })
            .size(CONTENT_SIZE)
            .build();

    // ---------- Update DTO ----------
    public static final DocumentUpdateDto docUpdate_valid_fromClient = DocumentUpdateDto.builder()
            .title("Updated Title")
            .tags(List.of("a","b"))
            .version(7L)
            .build();

    // ---------- Entities ----------
    public static final DocumentEntity doc_existing_fromRepository = DocumentEntity.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .ownerUid(VALID_OWNER_UID)
            .title("Existing")
            .version(1L)
            .build();

    public static final DocumentEntity doc_withBucketAndKey_fromRepository = DocumentEntity.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .ownerUid(VALID_OWNER_UID)
            .bucket(DOC_BUCKET)
            .objectKey(DOC_OBJECT_KEY)
            .title(DOC_TITLE_NON_BLANK)
            .mimeType(MIME_PDF)
            .size(CONTENT_SIZE)
            .build();

    // Doc senza mimeType e senza title -> per fallback nel download
    public static final DocumentEntity doc_withoutMimeAndTitle_fromRepository = DocumentEntity.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .ownerUid(VALID_OWNER_UID)
            .bucket(DOC_BUCKET)
            .objectKey(DOC_OBJECT_KEY)
            .build();

    public static final DocumentEntity doc_saved_afterUpdate = DocumentEntity.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .ownerUid(VALID_OWNER_UID)
            .title("Updated Title")
            .tags(List.of("a","b"))
            .version(7L)
            .updatedAt(Instant.now())
            .build();

    public static final DocumentEntity doc_instance1_fromRepository = DocumentEntity.builder()
            .id(new ObjectId().toHexString())
            .ownerUid(VALID_OWNER_UID)
            .title("Doc 1")
            .bucket(DOC_BUCKET)
            .objectKey("key-1")
            .build();

    public static final DocumentEntity doc_instance2_fromRepository = DocumentEntity.builder()
            .id(new ObjectId().toHexString())
            .ownerUid(VALID_OWNER_UID)
            .title("Doc 2")
            .bucket(DOC_BUCKET)
            .objectKey("key-2")
            .build();

    // ---------- Read DTOs ----------
    public static final DocumentReadDto documentReadDto_expected_fromSavedEntity = DocumentReadDto.builder()
            .id(VALID_DOC_ID)
            .title("any")
            .build();

    public static final DocumentReadDto docView_instance1_mapped = DocumentReadDto.builder()
            .id(doc_instance1_fromRepository.getId())
            .title(doc_instance1_fromRepository.getTitle())
            .build();

    public static final DocumentReadDto docView_instance2_mapped = DocumentReadDto.builder()
            .id(doc_instance2_fromRepository.getId())
            .title(doc_instance2_fromRepository.getTitle())
            .build();

    public static final DocumentReadDto docView_expected_fromFound = DocumentReadDto.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .title("Existing View")
            .build();

    public static final DocumentReadDto docView_mapped_afterUpdate = DocumentReadDto.builder()
            .id(VALID_EXISTENT_DOC_ID)
            .title("Updated Title")
            .tags(List.of("a","b"))
            .version(7L)
            .updatedAt(Instant.now())
            .build();
}
