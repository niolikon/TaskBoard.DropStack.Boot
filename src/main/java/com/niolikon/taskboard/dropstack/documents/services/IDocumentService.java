package com.niolikon.taskboard.dropstack.documents.services;

import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IDocumentService {

    DocumentReadDto create(String ownerUid, DocumentCreateMetadataDto metadata, DocumentCreateContentDto content);

    PageResponse<DocumentReadDto> readAll(String ownerUid, Pageable pageable);

    DocumentReadDto read(String ownerUid, String id);

    DocumentContentReadDto download(String ownerUid, String id);

    DocumentReadDto update(String ownerUid, String id, DocumentUpdateDto dto);

    void delete(String ownerUid, String id);
}
