package com.niolikon.taskboard.dropstack.documents.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentReadDto {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("ObjectKey")
    private String objectKey;

    @JsonProperty("Bucket")
    private String bucket;

    @JsonProperty("ETag")
    private String etag;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("MimeType")
    private String mimeType;

    @JsonProperty("Size")
    private Long size;

    @JsonProperty("Tags")
    private List<String> tags;

    @JsonProperty("CreatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonProperty("UpdatedAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @JsonProperty("Version")
    private Long version;
}
