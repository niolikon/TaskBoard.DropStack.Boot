package com.niolikon.taskboard.dropstack.documents.mappers;

import com.niolikon.taskboard.dropstack.documents.dto.DocumentAuditDto;
import com.niolikon.taskboard.dropstack.documents.model.DocumentAuditEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentAuditMapper {
    DocumentAuditDto toReadDto(DocumentAuditEntity entity);
}
