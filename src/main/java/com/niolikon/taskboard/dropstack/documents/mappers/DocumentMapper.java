package com.niolikon.taskboard.dropstack.documents.mappers;

import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.dropstack.documents.model.DocumentEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "etag", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    DocumentEntity toEntity(DocumentCreateMetadataDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "objectKey", ignore = true)
    @Mapping(target = "bucket", ignore = true)
    @Mapping(target = "etag", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "size", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    DocumentEntity toEntity(DocumentUpdateDto dto);

    DocumentReadDto toReadDto(DocumentEntity entity);
}
