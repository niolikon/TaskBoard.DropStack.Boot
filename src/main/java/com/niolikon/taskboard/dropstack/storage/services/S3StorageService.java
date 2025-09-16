package com.niolikon.taskboard.dropstack.storage.services;

import com.niolikon.taskboard.dropstack.storage.exceptions.StorageException;
import com.niolikon.taskboard.dropstack.storage.model.ObjectStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class S3StorageService implements IS3StorageService {

    private final S3Client s3;

    @Override
    public void upload(String bucket, String objectKey, InputStream data, long size, String contentType) {
        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();

            s3.putObject(putReq, RequestBody.fromInputStream(data, size));
        } catch (S3Exception e) {
            throw new StorageException("Upload failed for %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        try {
            GetObjectRequest getReq = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            return s3.getObject(getReq);
        } catch (NoSuchKeyException e) {
            throw new StorageException("Object not found: %s/%s".formatted(bucket, objectKey), e);
        } catch (S3Exception e) {
            throw new StorageException("Download failed for %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            s3.deleteObject(delReq);
        } catch (S3Exception e) {
            throw new StorageException("Delete failed for %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public Optional<ObjectStat> stat(String bucket, String objectKey) {
        try {
            HeadObjectRequest headReq = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .build();
            HeadObjectResponse headRes = s3.headObject(headReq);

            return Optional.of(new ObjectStat(
                    headRes.contentLength(),
                    headRes.eTag(),
                    headRes.contentType()
            ));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            throw new StorageException("Stat failed for %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public void setTags(String bucket, String objectKey, Map<String, String> tags) {
        try {
            Tagging tagging = Tagging.builder()
                    .tagSet(tags.entrySet().stream()
                            .map(entry -> Tag.builder()
                                    .key(entry.getKey())
                                    .value(entry.getValue())
                                    .build())
                            .toList())
                    .build();

            PutObjectTaggingRequest tagReq = PutObjectTaggingRequest.builder()
                    .bucket(bucket)
                    .key(objectKey)
                    .tagging(tagging)
                    .build();

            s3.putObjectTagging(tagReq);
        } catch (S3Exception e) {
            throw new StorageException("Set tags failed for %s/%s".formatted(bucket, objectKey), e);
        }
    }
}
