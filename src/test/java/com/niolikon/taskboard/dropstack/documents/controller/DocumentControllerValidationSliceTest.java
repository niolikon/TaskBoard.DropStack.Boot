package com.niolikon.taskboard.dropstack.documents.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriTemplate;

import java.util.stream.Stream;

import static com.niolikon.taskboard.dropstack.documents.controller.DocumentApiPaths.*;
import static com.niolikon.taskboard.dropstack.documents.controller.testdata.DocumentControllerTestData.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, DocumentControllerValidationSliceTest.TestSecurityBeans.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentControllerValidationSliceTest {

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

    Stream<Arguments> provideInvalidCreateRequests() throws JsonProcessingException {
        MockMultipartFile metadataOnly = new MockMultipartFile(
                PART_NAME_METADATA, "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata_valid_fromClient)
        );
        MockHttpServletRequestBuilder requestWithMissingFile = multipart(API_PATH_DOCUMENT_BASE)
                .file(metadataOnly)
                .with(jwtRequest_withValidRole);

        MockMultipartFile emptyFile = new MockMultipartFile(
                PART_NAME_FILE, MULTIPART_FILE_ORIGINAL_NAME, MIME_PDF, new byte[0]
        );
        MockMultipartFile validMetadata = new MockMultipartFile(
                PART_NAME_METADATA, "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata_valid_fromClient)
        );
        MockHttpServletRequestBuilder requestWithEmptyFile = multipart(API_PATH_DOCUMENT_BASE)
                .file(emptyFile).file(validMetadata)
                .with(jwtRequest_withValidRole);

        MockMultipartFile validFile = new MockMultipartFile(
                PART_NAME_FILE, MULTIPART_FILE_ORIGINAL_NAME, MIME_PDF, CONTENT_BYTES
        );
        MockHttpServletRequestBuilder requestWithMissingMetadata = multipart(API_PATH_DOCUMENT_BASE)
                .file(validFile)
                .with(jwtRequest_withValidRole);

        MockMultipartFile brokenMetadata = new MockMultipartFile(
                PART_NAME_METADATA, "", MediaType.APPLICATION_JSON_VALUE,
                "]not-json[".getBytes()
        );
        MockHttpServletRequestBuilder requestWithInvalidMetadataJson = multipart(API_PATH_DOCUMENT_BASE)
                .file(validFile).file(brokenMetadata)
                .with(jwtRequest_withValidRole);

        return Stream.of(
                Arguments.of(requestWithMissingFile),
                Arguments.of(requestWithEmptyFile),
                Arguments.of(requestWithMissingMetadata),
                Arguments.of(requestWithInvalidMetadataJson)
        );
    }

    static Stream<Arguments> provideDocumentRequestBodiedEndpoint() {
        MockHttpServletRequestBuilder updateEndpoint = put(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID))
                .with(jwtRequest_withValidRole)
                .contentType(MediaType.APPLICATION_JSON);
        return Stream.of(Arguments.of(updateEndpoint));
    }

    Stream<Arguments> provideInvalidUpdateBodies() {
        return Stream.of(Arguments.of(updateDto_invalidMissingVersion_fromClient));
    }

    Stream<Arguments> provideDocumentRequestBodiedEndpointWithInvalidBodies() {
        return provideDocumentRequestBodiedEndpoint().flatMap(endp ->
                provideInvalidUpdateBodies().map(body ->
                        Arguments.of(endp.get()[0], body.get()[0])
                )
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCreateRequests")
    void givenInvalidCreateMultipart_whenPosting_thenReturnsBadRequest(MockHttpServletRequestBuilder invalidCreateRequest) throws Exception {
        mockMvc.perform(invalidCreateRequest)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("provideDocumentRequestBodiedEndpointWithInvalidBodies")
    void givenInvalidUpdateBody_whenPutting_thenReturnsBadRequest(MockHttpServletRequestBuilder updateEndpoint, DocumentUpdateDto invalidBody) throws Exception {
        mockMvc.perform(updateEndpoint
                        .content(objectMapper.writeValueAsString(invalidBody)))
                .andExpect(status().isBadRequest());
    }
}
