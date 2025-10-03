package com.niolikon.taskboard.dropstack.documents.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentContentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateContentDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentCreateMetadataDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentUpdateDto;
import com.niolikon.taskboard.dropstack.documents.services.IDocumentService;
import com.niolikon.taskboard.dropstack.config.SecurityConfig;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.util.UriTemplate;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.niolikon.taskboard.dropstack.documents.controller.DocumentApiPaths.*;
import static com.niolikon.taskboard.dropstack.documents.controller.testdata.DocumentControllerTestData.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, DocumentControllerRelaySliceTest.TestSecurityBeans.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentControllerRelaySliceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private IDocumentService documentService;

    @TestConfiguration
    static class TestSecurityBeans {
        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
            return new JwtAuthenticationConverter();
        }
    }

    Stream<Arguments> provideEndpointRequestServiceMockAndRelayVerify() throws JsonProcessingException {
        MockMultipartFile filePart = new MockMultipartFile(
                PART_NAME_FILE, MULTIPART_FILE_ORIGINAL_NAME, MIME_PDF, CONTENT_BYTES
        );
        MockMultipartFile metadataPart = new MockMultipartFile(
                PART_NAME_METADATA, "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata_valid_fromClient)
        );
        MockHttpServletRequestBuilder createRequest = multipart(API_PATH_DOCUMENT_BASE)
                .file(filePart)
                .file(metadataPart)
                .with(jwtRequest_withValidRole);
        Consumer<IDocumentService> createServiceMockSetup = svc -> when(
                svc.create(eq(VALID_USER_ID), any(DocumentCreateMetadataDto.class), any(DocumentCreateContentDto.class))
        ).thenReturn(docView_expected_fromCreate);
        Consumer<IDocumentService> createServiceMockVerify = svc ->
                verify(svc).create(eq(VALID_USER_ID), any(DocumentCreateMetadataDto.class), any(DocumentCreateContentDto.class));

        MockHttpServletRequestBuilder readAllRequest = get(API_PATH_DOCUMENT_BASE)
                .with(jwtRequest_withValidRole);
        Consumer<IDocumentService> readAllServiceMockSetup = svc -> when(
                svc.readAll(eq(VALID_USER_ID), any())
        ).thenReturn(pageResponseDocumentReadDto_empty_fromRepository);
        Consumer<IDocumentService> readAllServiceMockVerify = svc ->
                verify(svc).readAll(eq(VALID_USER_ID), any());

        MockHttpServletRequestBuilder readRequest = get(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID))
                .with(jwtRequest_withValidRole);
        Consumer<IDocumentService> readServiceMockSetup = svc -> when(
                svc.read(VALID_USER_ID, VALID_DOC_ID)
        ).thenReturn(docView_instance1_fromRepository);
        Consumer<IDocumentService> readServiceMockVerify = svc ->
                verify(svc).read(VALID_USER_ID, VALID_DOC_ID);

        MockHttpServletRequestBuilder downloadRequest = get(new UriTemplate(API_PATH_DOCUMENT_CONTENT_BY_ID).expand(VALID_DOC_ID))
                .with(jwtRequest_withValidRole);
        Consumer<IDocumentService> downloadServiceMockSetup = svc -> when(
                svc.download(VALID_USER_ID, VALID_DOC_ID)
        ).thenReturn(new DocumentContentReadDto(new ByteArrayInputStream(CONTENT_BYTES), MIME_PDF, CONTENT_SIZE, DOC_TITLE));
        Consumer<IDocumentService> downloadServiceMockVerify = svc ->
                verify(svc).download(VALID_USER_ID, VALID_DOC_ID);

        MockHttpServletRequestBuilder updateRequest = put(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID))
                .with(jwtRequest_withValidRole)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto_valid_fromClient));
        Consumer<IDocumentService> updateServiceMockSetup = svc -> when(
                svc.update(eq(VALID_USER_ID), eq(VALID_DOC_ID), any(DocumentUpdateDto.class))
        ).thenReturn(docView_updated_fromService);
        Consumer<IDocumentService> updateServiceMockVerify = svc ->
                verify(svc).update(eq(VALID_USER_ID), eq(VALID_DOC_ID), any(DocumentUpdateDto.class));

        MockHttpServletRequestBuilder deleteRequest = delete(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID))
                .with(jwtRequest_withValidRole);
        Consumer<IDocumentService> deleteServiceMockVerify = svc ->
                verify(svc).delete(eq(VALID_USER_ID), any());

        return Stream.of(
                Arguments.of(createRequest,    createServiceMockSetup,    createServiceMockVerify),
                Arguments.of(readAllRequest,   readAllServiceMockSetup,   readAllServiceMockVerify),
                Arguments.of(readRequest,      readServiceMockSetup,      readServiceMockVerify),
                Arguments.of(downloadRequest,  downloadServiceMockSetup,  downloadServiceMockVerify),
                Arguments.of(updateRequest,    updateServiceMockSetup,    updateServiceMockVerify),
                Arguments.of(deleteRequest,    null,                      deleteServiceMockVerify)
        );
    }

    @ParameterizedTest
    @MethodSource("provideEndpointRequestServiceMockAndRelayVerify")
    void givenValidRequest_whenExecutingEndpoint_thenRequestIsRelayedToService(
            MockHttpServletRequestBuilder endpointRequest,
            Consumer<IDocumentService> serviceMockSetup,
            Consumer<IDocumentService> serviceMockVerify
    ) throws Exception {
        if (serviceMockSetup != null) serviceMockSetup.accept(documentService);
        mockMvc.perform(endpointRequest);
        assertDoesNotThrow(() -> serviceMockVerify.accept(documentService));
    }
}
