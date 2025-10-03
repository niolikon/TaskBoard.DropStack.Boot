package com.niolikon.taskboard.dropstack.storage.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class ObjectStat {
    long size;
    String etag;
    String contentType;
}
