package com.niolikon.taskboard.dropstack.documents.services;

import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.dropstack.documents.mappers.DocumentMapper;
import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import com.niolikon.taskboard.dropstack.documents.repositories.DocumentRepository;
import com.niolikon.taskboard.dropstack.storage.model.ObjectStat;
import com.niolikon.taskboard.dropstack.storage.services.IS3StorageService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import com.niolikon.taskboard.framework.exceptions.rest.client.EntityNotFoundRestException;
import com.niolikon.taskboard.framework.exceptions.rest.server.InternalServerErrorRestException;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService implements IDocumentService {
    private static final String DOCUMENT_NOT_FOUND = "Could not find document";
    private static final String DOCUMENT_NOT_UPDATED = "Could not update document";
    private static final String DOCUMENT_NOT_UPLOADED_TO_BUCKET = "Could not upload document to bucket";
    private static final String DOCUMENT_NOT_DELETED_FROM_BUCKET = "Could not delete document from bucket";
    private static final String DOCUMENT_CONTENT_DEFAULT_TYPE = "application/octet-stream";

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final IS3StorageService storage;

    @Value("${minio.bucket:taskboard-dropstack-docs}")
    private String defaultBucket;

    @Override
    public DocumentReadDto create(String ownerUid, DocumentCreateMetadataDto metadata, DocumentCreateContentDto content) {
        String bucket = defaultBucket;
        String objectKey = UUID.randomUUID().toString();
        Long contentSize = content.getSize();
        String contentType = metadata.getMimeType();

        try (InputStream in = content.getSource().getInputStream()) {
            storage.upload(bucket, objectKey, in, contentSize, contentType);
        } catch (Exception e) {
            throw new RuntimeException(DOCUMENT_NOT_UPLOADED_TO_BUCKET, e);
        }

        Optional<ObjectStat> statOpt = storage.stat(bucket, objectKey);
        Instant createdAndReadyInstant = Instant.now();

        DocumentEntity entity = documentMapper.toEntity(metadata);
        entity.setId(new ObjectId().toHexString());
        entity.setBucket(bucket);
        entity.setObjectKey(objectKey);
        entity.setSize(content.getSize());
        entity.setCreatedAt(createdAndReadyInstant);
        entity.setUpdatedAt(createdAndReadyInstant);
        entity.setOwnerUid(ownerUid);

        statOpt.ifPresent(stat -> entity.setEtag(stat.etag()));

        try {
            DocumentEntity saved = documentRepository.save(entity);
            return documentMapper.toReadDto(saved);
        } catch (RuntimeException ex) {
            try { storage.delete(bucket, objectKey); } catch (Exception ignore) {}
            throw ex;
        }
    }

    @Override
    public PageResponse<DocumentReadDto> readAll(String ownerUid, Pageable pageable) {
        Page<DocumentEntity> documents = documentRepository.findByOwnerUid(ownerUid, pageable);
        return new PageResponse<>(documents.map(documentMapper::toReadDto));
    }

    @Override
    public DocumentReadDto read(String ownerUid, String id) {
        DocumentEntity document = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DocumentService.DOCUMENT_NOT_FOUND));
        return documentMapper.toReadDto(document);
    }

    @Override
    public DocumentContentReadDto download(String ownerUid, String id) {
        var doc = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DOCUMENT_NOT_FOUND));

        Optional<ObjectStat> statOpt = storage.stat(doc.getBucket(), doc.getObjectKey());

        String contentType = statOpt.map(ObjectStat::contentType)
                .orElse(doc.getMimeType() != null ? doc.getMimeType() : DOCUMENT_CONTENT_DEFAULT_TYPE);
        Long contentLength = statOpt.map(ObjectStat::size).orElse(null);
        String filename = (doc.getTitle() != null && !doc.getTitle().isBlank())
                ? doc.getTitle()
                : doc.getObjectKey();
        InputStream in = storage.download(doc.getBucket(), doc.getObjectKey());

        return new DocumentContentReadDto(in, contentType, contentLength, filename);
    }

    @Override
    public DocumentReadDto update(String ownerUid, String id, DocumentUpdateDto dto) {
        DocumentEntity existing = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DocumentService.DOCUMENT_NOT_FOUND));
        Instant updateInstant = Instant.now();

        // Optimistic locking
        existing.setVersion(dto.getVersion());

        existing.setTitle(dto.getTitle());
        existing.setTags(dto.getTags());
        existing.setUpdatedAt(updateInstant);

        try {
            DocumentEntity saved = documentRepository.save(existing);
            return documentMapper.toReadDto(saved);
        } catch (OptimisticLockingFailureException e) {
            throw new InternalServerErrorRestException(DOCUMENT_NOT_UPDATED);
        }
    }

    @Override
    public void delete(String ownerUid, String id) {
        DocumentEntity document = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DocumentService.DOCUMENT_NOT_FOUND));

        try {
            storage.delete(document.getBucket(), document.getObjectKey());
        } catch (Exception e) {
            throw new RuntimeException(DOCUMENT_NOT_DELETED_FROM_BUCKET, e);
        }

        documentRepository.delete(document);
    }
}
