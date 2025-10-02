package com.niolikon.taskboard.dropstack.documents.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.niolikon.taskboard.dropstack.documents.controller.scenarios.MultipleDocumentsWithPaginationScenario;
import com.niolikon.taskboard.dropstack.documents.controller.scenarios.SingleDocumentScenario;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentUpdateDto;
import com.niolikon.taskboard.dropstack.storage.config.StorageConfig;
import com.niolikon.taskboard.dropstack.storage.services.IS3StorageService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import com.niolikon.taskboard.framework.test.annotations.WithIsolatedMongoTestScenario;
import com.niolikon.taskboard.framework.test.containers.MongoTestContainersConfig;
import com.niolikon.taskboard.framework.test.extensions.IsolatedMongoTestScenarioExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.List;

import static com.niolikon.taskboard.dropstack.documents.controller.DocumentApiPaths.API_PATH_DOCUMENT_BASE;
import static com.niolikon.taskboard.dropstack.documents.controller.DocumentApiPaths.API_PATH_DOCUMENT_BY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MongoTestContainersConfig.class)
@ExtendWith(IsolatedMongoTestScenarioExtension.class)
class DocumentControllerIT {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String VALID_USER_ROLE = "ROLE_USER";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StorageConfig storageConfig;

    @MockitoBean
    private IS3StorageService storageMock;

    @Test
    @WithIsolatedMongoTestScenario(dataClass = SingleDocumentScenario.class)
    void givenSingleDocument_whenReadAll_thenListWithSingleDocumentReturned() throws Exception {
        MvcResult mvc = mockMvc.perform(
                        get(API_PATH_DOCUMENT_BASE)
                                .with(authorizedUser(SingleDocumentScenario.USER_UUID))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andReturn();

        PageResponse<DocumentReadDto> page = parse(mvc, new TypeReference<>() {});
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getElementsTotal()).isEqualTo(1);
        DocumentReadDto first = page.getContent().get(0);
        assertThat(first.getId()).isEqualTo(SingleDocumentScenario.DOC_ID);
    }

    @Test
    @WithIsolatedMongoTestScenario(dataClass = SingleDocumentScenario.class)
    void givenSingleDocument_whenReadById_thenDocumentReturned() throws Exception {
        MvcResult mvc = mockMvc.perform(
                        get(API_PATH_DOCUMENT_BY_ID, SingleDocumentScenario.DOC_ID)
                                .with(authorizedUser(SingleDocumentScenario.USER_UUID))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Id").value(SingleDocumentScenario.DOC_ID))
                .andExpect(jsonPath("$.Title").value(SingleDocumentScenario.DOC_TITLE))
                .andReturn();

        DocumentReadDto dto = parse(mvc, new TypeReference<>() {});
        assertThat(dto.getVersion()).isEqualTo(SingleDocumentScenario.DOC_VERSION);
    }

    @Test
    @WithIsolatedMongoTestScenario(dataClass = SingleDocumentScenario.class)
    void givenCorrectVersion_whenUpdate_thenDocumentUpdatedAndVersionIncremented() throws Exception {
        DocumentUpdateDto update = DocumentUpdateDto.builder()
                .title("New title")
                .tags(List.of("a","b"))
                .version(SingleDocumentScenario.DOC_VERSION)
                .build();

        MvcResult mvc = mockMvc.perform(
                        put(API_PATH_DOCUMENT_BY_ID, SingleDocumentScenario.DOC_ID)
                                .with(authorizedUser(SingleDocumentScenario.USER_UUID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Title").value("New title"))
                .andReturn();

        DocumentReadDto dto = parse(mvc, new TypeReference<>() {});
        assertThat(dto.getVersion()).isEqualTo(SingleDocumentScenario.DOC_VERSION + 1);
    }

    @Test
    @WithIsolatedMongoTestScenario(dataClass = SingleDocumentScenario.class)
    void givenWrongVersion_whenUpdate_thenServerReturnsError() throws Exception {
        DocumentUpdateDto update = DocumentUpdateDto.builder()
                .title("Titolo sbagliato")
                .tags(List.of("x"))
                .version(999L) // wrong version
                .build();

        mockMvc.perform(
                        put(API_PATH_DOCUMENT_BY_ID, SingleDocumentScenario.DOC_ID)
                                .with(authorizedUser(SingleDocumentScenario.USER_UUID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(update))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isConflict());
    }

    @Test
    @WithIsolatedMongoTestScenario(dataClass = MultipleDocumentsWithPaginationScenario.class)
    void givenMoreDocumentsThanPageSize_whenReadAll_thenFirstPageReturnedWithMetadata() throws Exception {
        int page = 0, size = 10;

        mockMvc.perform(
                        get(API_PATH_DOCUMENT_BASE)
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                                .with(authorizedUser(MultipleDocumentsWithPaginationScenario.USER_UUID))
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(size))
                .andExpect(jsonPath("$.pageNumber").value(page))
                .andExpect(jsonPath("$.pageSize").value(size))
                .andExpect(jsonPath("$.elementsTotal").value(MultipleDocumentsWithPaginationScenario.TOTAL))
                .andExpect(jsonPath("$.pageTotal").value((int)Math.ceil((double) MultipleDocumentsWithPaginationScenario.TOTAL / size)));
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------
    private RequestPostProcessor authorizedUser(String subject) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject))
                .authorities(new SimpleGrantedAuthority(VALID_USER_ROLE));
    }

    private static <T> T parse(MvcResult mvcResult, TypeReference<T> typeRef) throws IOException {
        String jsonResponse = mvcResult
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(jsonResponse, typeRef);
    }
}
