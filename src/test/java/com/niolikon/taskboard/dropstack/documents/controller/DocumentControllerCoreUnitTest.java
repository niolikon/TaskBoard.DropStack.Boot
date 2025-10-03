package com.niolikon.taskboard.dropstack.documents.controller;

import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.dropstack.documents.services.IDocumentService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;

import static com.niolikon.taskboard.dropstack.documents.controller.testdata.DocumentControllerTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

class DocumentControllerCoreUnitTest {

    private Jwt stubJwt;
    private IDocumentService documentService;
    private DocumentController documentController;

    @BeforeEach
    void setUp() {
        stubJwt = mock(Jwt.class);
        when(stubJwt.getSubject()).thenReturn(JWT_SUBJECT_VALID_USER_ID);

        documentService = mock(IDocumentService.class);
        documentController = new DocumentController(documentService);
    }

    @Test
    void givenValidInput_whenCreateDocument_thenCreatedIsReturned() throws Exception {
        // Arrange
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(MULTIPART_FILE_SIZE);
        when(file.getOriginalFilename()).thenReturn(MULTIPART_FILE_ORIGINAL_NAME);

        DocumentCreateMetadataDto metadata = metadata_valid_fromClient;

        DocumentReadDto created = docView_expected_fromCreate;
        when(documentService.create(eq(JWT_SUBJECT_VALID_USER_ID), eq(metadata), any(DocumentCreateContentDto.class)))
                .thenReturn(created);

        ServletUriComponentsBuilder uriComponentsBuilder = mock(ServletUriComponentsBuilder.class);
        when(uriComponentsBuilder.path(DocumentApiPaths.MAPPING_PATH_DOCUMENT_BY_ID)).thenReturn(uriComponentsBuilder);
        UriComponents uriComponents = mock(UriComponents.class);
        when(uriComponentsBuilder.buildAndExpand(created.getId())).thenReturn(uriComponents);
        URI expectedLocation = new UriTemplate(DocumentApiPaths.API_PATH_DOCUMENT_BY_ID).expand(created.getId());
        when(uriComponents.toUri()).thenReturn(expectedLocation);

        // Act
        ResponseEntity<DocumentReadDto> response = documentController.create(stubJwt, file, metadata, uriComponentsBuilder);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isEqualTo(created);
        assertThat(response.getHeaders().getLocation()).isEqualTo(expectedLocation);
    }

    @Test
    void givenEmptyFile_whenCreateDocument_thenBadRequestIsReturned() throws Exception {
        // Arrange
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        // Act
        ResponseEntity<DocumentReadDto> response =
                documentController.create(stubJwt, emptyFile, metadata_valid_fromClient, mock(ServletUriComponentsBuilder.class));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(documentService);
    }

    @Test
    void givenValidInput_whenReadAllDocuments_thenOkIsReturned() {
        // Arrange
        List<DocumentReadDto> items = List.of(docView_instance1_fromRepository, docView_instance2_fromRepository);
        Page<DocumentReadDto> page = new PageImpl<>(items, pageable_firstPageSize10_fromClient, items.size());
        PageResponse<DocumentReadDto> pageResponse = new PageResponse<>(page);
        when(documentService.readAll(eq(JWT_SUBJECT_VALID_USER_ID), eq(pageable_firstPageSize10_fromClient))).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<DocumentReadDto>> response = documentController.readAll(stubJwt, pageable_firstPageSize10_fromClient);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(pageResponse);
    }

    @Test
    void givenValidInput_whenReadDocument_thenOkIsReturned() {
        // Arrange
        when(documentService.read(eq(JWT_SUBJECT_VALID_USER_ID), eq(VALID_DOC_ID))).thenReturn(docView_instance1_fromRepository);

        // Act
        ResponseEntity<DocumentReadDto> response = documentController.read(stubJwt, VALID_DOC_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(docView_instance1_fromRepository);
    }

    @Test
    void givenValidInput_whenDownloadDocument_thenOkWithHeadersAndBodyIsReturned() throws Exception {
        // Arrange
        ByteArrayInputStream input = new ByteArrayInputStream(CONTENT_BYTES);
        DocumentContentReadDto dlDto = new DocumentContentReadDto(input, MIME_PDF, CONTENT_SIZE, DOC_TITLE);
        when(documentService.download(eq(JWT_SUBJECT_VALID_USER_ID), eq(VALID_DOC_ID))).thenReturn(dlDto);

        // Act
        ResponseEntity<StreamingResponseBody> response = documentController.download(stubJwt, VALID_DOC_ID);

        // Assert – status + headers
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getHeaders().getFirst("Content-Type")).isEqualTo(MIME_PDF);
        assertThat(response.getHeaders().getFirst("Content-Length")).isEqualTo(String.valueOf(CONTENT_SIZE));
        String cd = response.getHeaders().getFirst("Content-Disposition");
        assertThat(cd).isNotBlank();
        assertThat(cd).contains("attachment");

        // Assert
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getBody().writeTo(out);
        assertThat(out.toByteArray()).isEqualTo(CONTENT_BYTES);
    }

    @Test
    void givenValidInput_whenUpdateDocument_thenOkIsReturned() {
        // Arrange
        when(documentService.update(eq(JWT_SUBJECT_VALID_USER_ID), eq(VALID_DOC_ID), eq(updateDto_valid_fromClient)))
                .thenReturn(docView_updated_fromService);

        // Act
        ResponseEntity<DocumentReadDto> response = documentController.update(stubJwt, VALID_DOC_ID, updateDto_valid_fromClient);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isEqualTo(docView_updated_fromService);
    }

    @Test
    void givenValidInput_whenDeleteDocument_thenNoContentIsReturned() {
        // Act
        ResponseEntity<Void> response = documentController.delete(stubJwt, VALID_DOC_ID);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT);
        verify(documentService).delete(JWT_SUBJECT_VALID_USER_ID, VALID_DOC_ID);
    }
}
