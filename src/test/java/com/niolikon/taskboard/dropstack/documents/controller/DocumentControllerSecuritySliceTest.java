package com.niolikon.taskboard.dropstack.documents.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Import({SecurityConfig.class, DocumentControllerSecuritySliceTest.TestSecurityBeans.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DocumentControllerSecuritySliceTest {

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

    Stream<Arguments> provideSecuredEndpointRequest() throws JsonProcessingException {
        // POST /api/Documents (multipart)
        MockMultipartFile filePart = new MockMultipartFile(
                PART_NAME_FILE, MULTIPART_FILE_ORIGINAL_NAME, MIME_PDF, CONTENT_BYTES
        );
        MockMultipartFile metadataPart = new MockMultipartFile(
                PART_NAME_METADATA, "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(metadata_valid_fromClient)
        );
        MockHttpServletRequestBuilder createRequest = multipart(API_PATH_DOCUMENT_BASE)
                .file(filePart)
                .file(metadataPart);

        // GET /api/Documents
        MockHttpServletRequestBuilder readAllRequest = get(API_PATH_DOCUMENT_BASE);

        // GET /api/Documents/{id}
        MockHttpServletRequestBuilder readRequest =
                get(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID));

        // GET /api/Documents/{id}/content
        MockHttpServletRequestBuilder downloadRequest =
                get(new UriTemplate(API_PATH_DOCUMENT_CONTENT_BY_ID).expand(VALID_DOC_ID));

        // PUT /api/Documents/{id}
        MockHttpServletRequestBuilder updateRequest =
                put(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto_valid_fromClient));

        // DELETE /api/Documents/{id}
        MockHttpServletRequestBuilder deleteRequest =
                delete(new UriTemplate(API_PATH_DOCUMENT_BY_ID).expand(VALID_DOC_ID));

        return Stream.of(
                Arguments.of(createRequest),
                Arguments.of(readAllRequest),
                Arguments.of(readRequest),
                Arguments.of(downloadRequest),
                Arguments.of(updateRequest),
                Arguments.of(deleteRequest)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSecuredEndpointRequest")
    void givenNoAuthorities_whenAccessingSecuredEndpoint_thenReturnsForbidden(MockHttpServletRequestBuilder endpointRequest) throws Exception {
        mockMvc.perform(endpointRequest.with(jwtRequest_withoutAuthorities))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("provideSecuredEndpointRequest")
    void givenWrongAuthorities_whenAccessingSecuredEndpoint_thenReturnsForbidden(MockHttpServletRequestBuilder endpointRequest) throws Exception {
        mockMvc.perform(endpointRequest.with(jwtRequest_withInvalidRole))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("provideSecuredEndpointRequest")
    void givenNoJwt_whenAccessingSecuredEndpoint_thenReturnsUnauthorized(MockHttpServletRequestBuilder endpointRequest) throws Exception {
        mockMvc.perform(endpointRequest)
                .andExpect(status().isUnauthorized());
    }
}
