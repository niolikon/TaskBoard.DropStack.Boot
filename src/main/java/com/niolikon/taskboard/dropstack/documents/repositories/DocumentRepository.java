package com.niolikon.taskboard.dropstack.documents.repositories;

import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {

    Page<DocumentEntity> findByOwnerUid(String ownerUid, Pageable pageable);

    Optional<DocumentEntity> findByIdAndOwnerUid(String id, String ownerUid);

}

