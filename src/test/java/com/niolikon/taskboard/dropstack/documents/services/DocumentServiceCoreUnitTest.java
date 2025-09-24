package com.niolikon.taskboard.dropstack.documents.services;

import com.niolikon.taskboard.dropstack.documents.dto.DocumentContentReadDto;
import com.niolikon.taskboard.dropstack.documents.dto.DocumentReadDto;
import com.niolikon.taskboard.dropstack.documents.mappers.DocumentMapper;
import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import com.niolikon.taskboard.dropstack.documents.repositories.DocumentRepository;
import com.niolikon.taskboard.dropstack.storage.services.IS3StorageService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import com.niolikon.taskboard.framework.exceptions.rest.client.EntityNotFoundRestException;
import com.niolikon.taskboard.framework.exceptions.rest.server.InternalServerErrorRestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.niolikon.taskboard.dropstack.documents.services.testdata.DocumentServiceTestData.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceCoreUnitTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private IS3StorageService storage;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, documentMapper, storage, DEFAULT_BUCKET_FOR_TESTS);
    }

    @Test
    void givenValidMetadataAndContent_whenCreate_thenPersistsEntityAndReturnsReadDto() {
        // Arrange
        when(storage.stat(eq(DEFAULT_BUCKET_FOR_TESTS), anyString())).
                thenReturn(Optional.of(objectStat_withEtag_andContentType));
        when(documentMapper.toEntity(metadata_valid_fromClient)).thenReturn(new DocumentEntity());
        when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(inv -> {
            DocumentEntity e = inv.getArgument(0);
            e.setId(VALID_DOC_ID);
            return e;
        });
        DocumentReadDto expectedView = documentReadDto_expected_fromSavedEntity;
        when(documentMapper.toReadDto(any(DocumentEntity.class))).thenReturn(expectedView);

        // Act
        DocumentReadDto result = documentService.create(VALID_OWNER_UID, metadata_valid_fromClient, content_valid_fromClient);

        // Assert
        assertThat(result).isEqualTo(expectedView);

        ArgumentCaptor<String> objectKeyCap = ArgumentCaptor.forClass(String.class);
        verify(storage).upload(eq(DEFAULT_BUCKET_FOR_TESTS), objectKeyCap.capture(), any(InputStream.class), eq(CONTENT_SIZE), eq(MIME_PDF));

        ArgumentCaptor<DocumentEntity> savedCap = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(savedCap.capture());
        assertThat(savedCap.getValue())
                .extracting(DocumentEntity::getBucket, DocumentEntity::getObjectKey, DocumentEntity::getSize,
                        DocumentEntity::getOwnerUid, DocumentEntity::getEtag)
                        .containsExactly(
                                DEFAULT_BUCKET_FOR_TESTS, objectKeyCap.getValue(), CONTENT_SIZE, VALID_OWNER_UID,
                                ETAG_VALUE
                        );

        verify(storage).stat(DEFAULT_BUCKET_FOR_TESTS, objectKeyCap.getValue());
        verify(documentMapper).toEntity(metadata_valid_fromClient);
        verify(documentMapper).toReadDto(any(DocumentEntity.class));
        verifyNoMoreInteractions(documentRepository, documentMapper, storage);
    }

    @Test
    void givenUploadFails_whenCreate_thenThrowsRuntime_andDeletesFromBucketIfNeededIsNotCalled() {
        // Arrange
        doThrow(new RuntimeException("network")).when(storage)
                .upload(eq(DEFAULT_BUCKET_FOR_TESTS), anyString(), any(InputStream.class), eq(CONTENT_SIZE), eq(MIME_PDF));

        // Act & Assert
        assertThatThrownBy(() -> documentService.create(VALID_OWNER_UID, metadata_valid_fromClient, content_valid_fromClient))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(DocumentService.DOCUMENT_NOT_UPLOADED_TO_BUCKET);

        verify(storage).upload(eq(DEFAULT_BUCKET_FOR_TESTS), anyString(), any(InputStream.class), eq(CONTENT_SIZE), eq(MIME_PDF));

        verifyNoInteractions(documentRepository, documentMapper);
        verifyNoMoreInteractions(storage);
    }

    @Test
    void givenSaveFails_afterUpload_whenCreate_thenReThrows_andAttemptsCleanupDelete() {
        // Arrange
        when(documentMapper.toEntity(metadata_valid_fromClient)).thenReturn(new DocumentEntity());
        when(storage.stat(eq(DEFAULT_BUCKET_FOR_TESTS), anyString())).thenReturn(Optional.empty());
        when(documentRepository.save(any(DocumentEntity.class))).thenThrow(new RuntimeException("db down"));

        // Act
        assertThatThrownBy(() -> documentService.create(VALID_OWNER_UID, metadata_valid_fromClient, content_valid_fromClient))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        // Assert
        ArgumentCaptor<String> objectKeyCap = ArgumentCaptor.forClass(String.class);

        verify(storage).upload(eq(DEFAULT_BUCKET_FOR_TESTS), objectKeyCap.capture(), any(InputStream.class), eq(CONTENT_SIZE), eq(MIME_PDF));
        verify(storage).stat(DEFAULT_BUCKET_FOR_TESTS, objectKeyCap.getValue());
        verify(storage).delete(DEFAULT_BUCKET_FOR_TESTS, objectKeyCap.getValue());
        verify(documentMapper).toEntity(metadata_valid_fromClient);
        verify(documentRepository).save(any(DocumentEntity.class));
        verifyNoMoreInteractions(documentRepository, documentMapper, storage);
    }

    @Test
    void givenMultipleDocumentsExist_whenReadAll_thenReturnsMappedPageResponse() {
        // Arrange
        List<DocumentEntity> docs = List.of(doc_instance1_fromRepository, doc_instance2_fromRepository);
        Page<DocumentEntity> paged = new PageImpl<>(docs, pageable_firstPageSize10_fromClient, docs.size());
        when(documentRepository.findByOwnerUid(VALID_OWNER_UID, pageable_firstPageSize10_fromClient)).thenReturn(paged);
        when(documentMapper.toReadDto(any(DocumentEntity.class))).thenReturn(docView_instance1_mapped, docView_instance2_mapped);

        // Act
        PageResponse<DocumentReadDto> result = documentService.readAll(VALID_OWNER_UID, pageable_firstPageSize10_fromClient);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(List.of(docView_instance1_mapped, docView_instance2_mapped));

        verify(documentRepository).findByOwnerUid(VALID_OWNER_UID, pageable_firstPageSize10_fromClient);
        verify(documentMapper, times(2)).toReadDto(any(DocumentEntity.class));
    }

    @Test
    void givenExistingDocument_whenRead_thenReturnsReadDto() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_existing_fromRepository));
        when(documentMapper.toReadDto(doc_existing_fromRepository)).thenReturn(docView_expected_fromFound);

        // Act
        DocumentReadDto result = documentService.read(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID);

        // Assert
        assertThat(result).isEqualTo(docView_expected_fromFound);

        verify(documentRepository).findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID);
        verify(documentMapper).toReadDto(doc_existing_fromRepository);
    }

    @Test
    void givenNonExistingDocument_whenRead_thenThrowsEntityNotFound() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.read(VALID_OWNER_UID, VALID_NON_EXISTENT_DOC_ID))
                .isInstanceOf(EntityNotFoundRestException.class);

        verify(documentRepository).findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID);
        verifyNoMoreInteractions(documentRepository, documentMapper);
    }

    @Test
    void givenExistingDocument_whenDownload_andStatPresent_thenReturnsContentDtoWithStatValues() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_withBucketAndKey_fromRepository));
        when(storage.stat(DOC_BUCKET, DOC_OBJECT_KEY)).thenReturn(Optional.of(objectStat_withEtag_andContentType));
        when(storage.download(DOC_BUCKET, DOC_OBJECT_KEY)).thenReturn(new ByteArrayInputStream(CONTENT_BYTES));

        // Act
        DocumentContentReadDto dto = documentService.download(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID);

        // Assert
        assertThat(dto)
                .extracting(DocumentContentReadDto::getContentType, DocumentContentReadDto::getContentLength, DocumentContentReadDto::getFilename)
                .containsExactly(MIME_PDF, CONTENT_SIZE, DOC_TITLE_NON_BLANK);

        verify(storage).download(DOC_BUCKET, DOC_OBJECT_KEY);
    }

    @Test
    void givenExistingDocument_whenDownload_andStatAbsent_thenFallsBackToDocMimeOrDefault() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_withoutMimeAndTitle_fromRepository));
        when(storage.stat(DOC_BUCKET, DOC_OBJECT_KEY)).thenReturn(Optional.empty());
        when(storage.download(DOC_BUCKET, DOC_OBJECT_KEY)).thenReturn(new ByteArrayInputStream(CONTENT_BYTES));

        // Act
        DocumentContentReadDto dto = documentService.download(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID);

        // Assert
        assertThat(dto)
                .extracting(DocumentContentReadDto::getContentType, DocumentContentReadDto::getFilename)
                .containsExactly(DOCUMENT_CONTENT_DEFAULT_TYPE, DOC_OBJECT_KEY);
        assertThat(dto.getContentLength()).isNull();
    }

    @Test
    void givenExistingDocument_whenUpdate_thenSavesAndReturnsMappedDto() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_existing_fromRepository));
        when(documentRepository.save(any(DocumentEntity.class))).thenReturn(doc_saved_afterUpdate);
        when(documentMapper.toReadDto(doc_saved_afterUpdate)).thenReturn(docView_mapped_afterUpdate);

        // Act
        DocumentReadDto result = documentService.update(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID, docUpdate_valid_fromClient);

        // Assert
        assertThat(result).isEqualTo(docView_mapped_afterUpdate);

        ArgumentCaptor<DocumentEntity> savedCap = ArgumentCaptor.forClass(DocumentEntity.class);
        verify(documentRepository).save(savedCap.capture());
        DocumentEntity saved = savedCap.getValue();
        assertThat(saved)
                .extracting(DocumentEntity::getTitle, DocumentEntity::getTags, DocumentEntity::getVersion)
                .containsExactly(docUpdate_valid_fromClient.getTitle(), docUpdate_valid_fromClient.getTags(), docUpdate_valid_fromClient.getVersion());
        assertThat(saved.getUpdatedAt()).isNotNull();

        verify(documentMapper).toReadDto(doc_saved_afterUpdate);
        verifyNoMoreInteractions(documentRepository, documentMapper);
    }

    @Test
    void givenNonExistingDocument_whenUpdate_thenThrowsEntityNotFound() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.update(VALID_OWNER_UID, VALID_NON_EXISTENT_DOC_ID, docUpdate_valid_fromClient))
                .isInstanceOf(EntityNotFoundRestException.class);

        verify(documentRepository).findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID);
        verifyNoMoreInteractions(documentRepository, documentMapper);
    }

    @Test
    void givenOptimisticLockingFailure_whenUpdate_thenThrowsInternalServerError() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_existing_fromRepository));
        when(documentRepository.save(any(DocumentEntity.class)))
                .thenThrow(new OptimisticLockingFailureException("stale"));

        // Act & Assert
        assertThatThrownBy(() -> documentService.update(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID, docUpdate_valid_fromClient))
                .isInstanceOf(InternalServerErrorRestException.class)
                .hasMessageContaining(DocumentService.DOCUMENT_NOT_UPDATED);

        verify(documentRepository).findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID);
        verify(documentRepository).save(any(DocumentEntity.class));
        verifyNoMoreInteractions(documentRepository, documentMapper);
    }

    @Test
    void givenExistingDocument_whenDelete_thenRemovesFromBucketAndRepository() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_withBucketAndKey_fromRepository));

        // Act
        documentService.delete(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID);

        // Assert
        verify(storage).delete(DOC_BUCKET, DOC_OBJECT_KEY);
        verify(documentRepository).delete(doc_withBucketAndKey_fromRepository);
    }

    @Test
    void givenNonExistingDocument_whenDelete_thenThrowsEntityNotFound() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> documentService.delete(VALID_OWNER_UID, VALID_NON_EXISTENT_DOC_ID))
                .isInstanceOf(EntityNotFoundRestException.class);

        verify(documentRepository).findByIdAndOwnerUid(VALID_NON_EXISTENT_DOC_ID, VALID_OWNER_UID);
        verifyNoMoreInteractions(documentRepository, documentMapper, storage);
    }

    @Test
    void givenStorageDeleteFails_whenDelete_thenThrowsRuntime() {
        // Arrange
        when(documentRepository.findByIdAndOwnerUid(VALID_EXISTENT_DOC_ID, VALID_OWNER_UID))
                .thenReturn(Optional.of(doc_withBucketAndKey_fromRepository));
        doThrow(new RuntimeException("minio unavailable"))
                .when(storage).delete(DOC_BUCKET, DOC_OBJECT_KEY);

        // Act & Assert
        assertThatThrownBy(() -> documentService.delete(VALID_OWNER_UID, VALID_EXISTENT_DOC_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(DocumentService.DOCUMENT_NOT_DELETED_FROM_BUCKET);

        verify(storage).delete(DOC_BUCKET, DOC_OBJECT_KEY);
        verify(documentRepository, never()).delete(any(DocumentEntity.class));
    }
}
