package com.niolikon.taskboard.dropstack.storage.services;

import com.niolikon.taskboard.dropstack.storage.model.ObjectStat;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public interface IS3StorageService {

    void upload(String bucket, String objectKey, InputStream data, long size, String contentType);

    InputStream download(String bucket, String objectKey);

    void delete(String bucket, String objectKey);

    Optional<ObjectStat> stat(String bucket, String objectKey);

    void setTags(String bucket, String objectKey, Map<String, String> tags);
}
