package com.niolikon.taskboard.dropstack.documents.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@Document(collection = "document_audits")
public class DocumentAuditEntity {

    @Id
    private String id;

    @Field("documentId")
    private String documentId;

    @Field("type")
    private String type; // es. "CREATE", "UPDATE", "DELETE"

    @Field("payload")
    private Map<String, Object> payload;

    @Field("at")
    private Instant at;

    @Field("by")
    private String by;
}
