package com.niolikon.taskboard.dropstack.documents.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentAuditDto {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("DocumentId")
    private String documentId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("Payload")
    private Map<String, Object> payload;

    @JsonProperty("At")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant at;

    @JsonProperty("By")
    private String by;
}
