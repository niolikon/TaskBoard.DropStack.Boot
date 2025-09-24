package com.niolikon.taskboard.dropstack.documents.controller.testdata;

import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateMetadataDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentUpdateDto;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public class DocumentControllerTestData {
    private DocumentControllerTestData() {}

    public static final String VALID_USER_ID = "user-123";
    public static final String VALID_USER_ROLE = "ROLE_USER";
    public static final String INVALID_USER_ROLE = "ROLE_CITIZEN";
    public static final String VALID_DOC_ID = new ObjectId().toHexString();
    public static final String JWT_SUBJECT_VALID_USER_ID = VALID_USER_ID;
    public static final String JWT_AUTHORITIES_VALID_USER_ROLE = VALID_USER_ROLE;
    public static final String JWT_AUTHORITIES_INVALID_USER_ROLE = INVALID_USER_ROLE;

    public static final String MIME_PDF = "application/pdf";
    public static final long MULTIPART_FILE_SIZE = 1024L;
    public static final String MULTIPART_FILE_ORIGINAL_NAME = "sample.pdf";
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

    public static final DocumentUpdateDto updateDto_invalidMissingVersion_fromClient = DocumentUpdateDto.builder()
            .title("Updated Title")
            .tags(List.of("x","y"))
            .version(null)
            .build();

    public static final DocumentReadDto docView_updated_fromService = DocumentReadDto.builder()
            .id(VALID_DOC_ID)
            .title("Updated Title")
            .tags(List.of("x","y"))
            .version(3L)
            .build();

    public static final PageResponse<DocumentReadDto> pageResponseDocumentReadDto_empty_fromRepository =
            new PageResponse<>(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

    public static final RequestPostProcessor jwtRequest_withoutAuthorities = jwt()
            .jwt(jwt -> jwt.subject(VALID_USER_ID));
    public static final RequestPostProcessor jwtRequest_withInvalidRole = jwt()
            .jwt(jwt -> jwt.subject(VALID_USER_ID))
            .authorities(new SimpleGrantedAuthority(JWT_AUTHORITIES_INVALID_USER_ROLE));
    public static final RequestPostProcessor jwtRequest_withValidRole = jwt()
            .jwt(j -> j.subject(JWT_SUBJECT_VALID_USER_ID))
            .authorities(new SimpleGrantedAuthority(JWT_AUTHORITIES_VALID_USER_ROLE));
}
