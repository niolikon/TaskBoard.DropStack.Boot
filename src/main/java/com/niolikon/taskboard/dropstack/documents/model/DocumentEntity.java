package com.niolikon.taskboard.dropstack.documents.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class DocumentEntity {

    @Id
    @EqualsAndHashCode.Exclude
    private String id;

    @Field("objectKey")
    @EqualsAndHashCode.Exclude
    private String objectKey; // S3 foreing key

    @Field("bucket")
    @EqualsAndHashCode.Exclude
    private String bucket; // S3 bucket

    @Field("etag")
    private String etag; // S3 file tag (file modification detection)

    @Field("title")
    private String title;

    @Field("mimeType")
    private String mimeType;

    @Field("size")
    private Long size;

    @Field("tags")
    private java.util.List<String> tags;

    @Field("createdAt")
    @EqualsAndHashCode.Exclude
    private Instant createdAt;

    @Field("updatedAt")
    @EqualsAndHashCode.Exclude
    private Instant updatedAt;

    @Version
    private Long version; // Spring Data optimistic locking

    @EqualsAndHashCode.Include
    @Setter
    private String ownerUid;

    @Field("categoryCode")
    @EqualsAndHashCode.Exclude
    private String categoryCode;

    @Field("checkedInAt")
    @EqualsAndHashCode.Exclude
    private Instant checkedInAt;
}
