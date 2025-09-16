package com.niolikon.taskboard.dropstack.documents.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentContentReadDto {
    InputStream stream;
    String contentType;
    Long contentLength;
    String filename;
}
