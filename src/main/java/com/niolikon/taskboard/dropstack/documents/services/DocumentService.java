package com.niolikon.taskboard.dropstack.documents.services;

import com.mongodb.client.result.UpdateResult;
import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.dropstack.documents.mappers.DocumentMapper;
import com.niolikon.taskboard.dropstack.documents.model.DocumentAuditEntity;
import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import com.niolikon.taskboard.dropstack.documents.repositories.DocumentAuditRepository;
import com.niolikon.taskboard.dropstack.documents.repositories.DocumentRepository;
import com.niolikon.taskboard.dropstack.storage.model.ObjectStat;
import com.niolikon.taskboard.dropstack.storage.services.IS3StorageService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import com.niolikon.taskboard.framework.exceptions.rest.client.ConflictRestException;
import com.niolikon.taskboard.framework.exceptions.rest.client.EntityNotFoundRestException;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class DocumentService implements IDocumentService {
    static final String DOCUMENT_NOT_FOUND = "Could not find document";
    static final String DOCUMENT_NOT_UPDATED = "Could not update document";
    static final String DOCUMENT_NOT_UPLOADED_TO_BUCKET = "Could not upload document to bucket";
    static final String DOCUMENT_NOT_DELETED_FROM_BUCKET = "Could not delete document from bucket";
    static final String DOCUMENT_CONTENT_DEFAULT_TYPE = "application/octet-stream";

    private final DocumentRepository documentRepository;
    private final DocumentAuditRepository documentAuditRepository;
    private final DocumentMapper documentMapper;
    private final IS3StorageService storage;
    private final String defaultBucket;

    private final MongoTemplate mongoTemplate;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentAuditRepository documentAuditRepository,
                           DocumentMapper documentMapper,
                           IS3StorageService storage,
                           @Value("${minio.bucket:taskboard-dropstack-docs}") String defaultBucket,
                           MongoTemplate mongoTemplate) {
        this.documentRepository = documentRepository;
        this.documentAuditRepository = documentAuditRepository;
        this.documentMapper = documentMapper;
        this.storage = storage;
        this.defaultBucket = defaultBucket;
        this.mongoTemplate = mongoTemplate;
    }

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

        statOpt.ifPresent(stat -> entity.setEtag(stat.getEtag()));

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
        DocumentEntity doc = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DOCUMENT_NOT_FOUND));

        Optional<ObjectStat> statOpt = storage.stat(doc.getBucket(), doc.getObjectKey());

        String contentType = statOpt.map(ObjectStat::getContentType)
                .orElse(doc.getMimeType() != null ? doc.getMimeType() : DOCUMENT_CONTENT_DEFAULT_TYPE);
        Long contentLength = statOpt.map(ObjectStat::getSize).orElse(null);
        String filename = (doc.getTitle() != null && !doc.getTitle().isBlank())
                ? doc.getTitle()
                : doc.getObjectKey();
        InputStream in = storage.download(doc.getBucket(), doc.getObjectKey());

        return new DocumentContentReadDto(in, contentType, contentLength, filename);
    }

    @Override
    public DocumentReadDto checkIn(String ownerUid, String id, DocumentCheckinDto dto) {
        DocumentEntity doc = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DOCUMENT_NOT_FOUND));

        if (!doc.getVersion().equals(dto.getVersion())) {
            throw new ConflictRestException(DOCUMENT_NOT_UPDATED);
        }

        Instant now = Instant.now();
        String oldCategory = doc.getCategoryCode();

        // Partial update
        Query selectDocumentByIdAndVersion = new Query(
                where("_id").is(id).and("version").is(dto.getVersion()));

        Update setCategoryWithPartialUpdate = new Update()
                .set("categoryCode", dto.getCategoryCode())
                .set("checkedInAt", now)
                .set("checkedInBy", ownerUid)
                .set("updatedAt", now)
                .inc("version", 1);

        UpdateResult result = mongoTemplate.updateFirst(selectDocumentByIdAndVersion, setCategoryWithPartialUpdate, DocumentEntity.class);
        if (result.getModifiedCount() == 0) {
            throw new ConflictRestException(DOCUMENT_NOT_UPDATED);
        }

        DocumentEntity updated = documentRepository.findByIdAndOwnerUid(id, ownerUid)
                .orElseThrow(() -> new EntityNotFoundRestException(DOCUMENT_NOT_FOUND));

        DocumentAuditEntity audit = new DocumentAuditEntity();
        audit.setId(new ObjectId().toHexString());
        audit.setDocumentId(updated.getId());
        audit.setType("CHECKIN");
        audit.setAt(now);
        audit.setBy(ownerUid);
        audit.setPayload(Map.of("oldCategoryCode", oldCategory, "newCategoryCode", updated.getCategoryCode()));
        documentAuditRepository.save(audit);

        return documentMapper.toReadDto(updated);
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
            throw new ConflictRestException(DOCUMENT_NOT_UPDATED);
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
