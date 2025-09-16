package com.niolikon.taskboard.dropstack.documents.repositories;

import com.niolikon.taskboard.dropstack.documents.model.DocumentAuditEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentAuditRepository extends MongoRepository<DocumentAuditEntity, String> {

    List<DocumentAuditEntity> findByDocumentIdOrderByAtDesc(String documentId);
}

