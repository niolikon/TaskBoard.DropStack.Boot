package com.niolikon.taskboard.dropstack.documents.controller.testdata;

import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateMetadataDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentUpdateDto;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DocumentControllerTestData {
    private DocumentControllerTestData() {}

    public static final String JWT_SUBJECT_VALID_USER_ID = "user-123";
    public static final String VALID_DOC_ID = new ObjectId().toHexString();

    public static final String MIME_PDF = "application/pdf";
    public static final long FILE_SIZE = 1024L;
    public static final String FILE_ORIGINAL_NAME = "sample.pdf";
    public static final String DOC_TITLE = "downloaded.pdf";
    public static final byte[] CONTENT_BYTES = "hello-doc".getBytes(StandardCharsets.UTF_8);
    public static final Long CONTENT_SIZE = (long) CONTENT_BYTES.length;

    public static final Pageable pageable_firstPageSize10_fromClient = PageRequest.of(0, 10);

    // Input DTO
    public static final DocumentCreateMetadataDto metadata_valid_fromClient = DocumentCreateMetadataDto.builder()
            .mimeType(MIME_PDF)
            .title("My Doc")
            .tags(List.of("a","b"))
            .build();

    // Views
    public static final DocumentReadDto docView_expected_fromCreate = DocumentReadDto.builder()
            .id(VALID_DOC_ID)
            .title("My Doc")
            .build();

    public static final DocumentReadDto docView_instance1_fromRepository = DocumentReadDto.builder()
            .id(VALID_DOC_ID)
            .title("One")
            .build();

    public static final DocumentReadDto docView_instance2_fromRepository = DocumentReadDto.builder()
            .id(new ObjectId().toHexString())
            .title("Two")
            .build();

    public static final DocumentUpdateDto updateDto_valid_fromClient = DocumentUpdateDto.builder()
            .title("Updated Title")
            .tags(List.of("x","y"))
            .version(3L)
            .build();

    public static final DocumentReadDto docView_updated = DocumentReadDto.builder()
            .id(VALID_DOC_ID)
            .title("Updated Title")
            .tags(List.of("x","y"))
            .version(3L)
            .build();
}
