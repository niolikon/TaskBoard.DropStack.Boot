package com.niolikon.taskboard.dropstack.documents.controller;

import com.niolikon.taskboard.dropstack.documents.dto.*;
import com.niolikon.taskboard.dropstack.documents.services.IDocumentService;
import com.niolikon.taskboard.framework.data.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.niolikon.taskboard.dropstack.documents.controller.DocumentApiPaths.*;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(MAPPING_PATH_DOCUMENT_BASE)
public class DocumentController {

    private final IDocumentService documentService;

    public DocumentController(IDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentReadDto> create(@AuthenticationPrincipal Jwt jwt,
                                                  @RequestParam(PART_NAME_FILE) MultipartFile file,
                                                  @Valid @RequestPart(PART_NAME_METADATA) DocumentCreateMetadataDto metadata,
                                                  ServletUriComponentsBuilder uriComponentsBuilder) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String ownerUid = jwt.getSubject();

        DocumentCreateContentDto content = DocumentCreateContentDto.builder()
                .source(file)
                .size(file.getSize())
                .originalFilename(file.getOriginalFilename())
                .build();

        DocumentReadDto created = documentService.create(ownerUid, metadata, content);

        URI location = uriComponentsBuilder
                .path(MAPPING_PATH_DOCUMENT_BY_ID)
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<DocumentReadDto>> readAll(@AuthenticationPrincipal Jwt jwt,
                                                                 @PageableDefault(size = 20) Pageable pageable) {
        String ownerUid = jwt.getSubject();
        PageResponse<DocumentReadDto> documents = documentService.readAll(ownerUid, pageable);
        return ok().body(documents);
    }

    @GetMapping(MAPPING_PATH_DOCUMENT_BY_ID)
    public ResponseEntity<DocumentReadDto> read(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable(PATH_VARIABLE_DOCUMENT_ID) String id) {
        String ownerUid = jwt.getSubject();
        DocumentReadDto document = documentService.read(ownerUid, id);
        return ok().body(document);
    }

    @GetMapping(MAPPING_PATH_DOCUMENT_CONTENT_BY_ID)
    public ResponseEntity<StreamingResponseBody> download(@AuthenticationPrincipal Jwt jwt,
                                                          @PathVariable(PATH_VARIABLE_DOCUMENT_ID) String id) {
        String ownerUid = jwt.getSubject();
        DocumentContentReadDto dl = documentService.download(ownerUid, id);

        String cd = ContentDisposition.attachment()
                .filename(dl.getFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        var builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .header(HttpHeaders.CONTENT_TYPE, dl.getContentType());

        if (dl.getContentLength() != null && dl.getContentLength() >= 0) {
            builder = builder.header(HttpHeaders.CONTENT_LENGTH, String.valueOf(dl.getContentLength()));
        }

        StreamingResponseBody body = out -> {
            try (InputStream is = dl.getStream()) {
                is.transferTo(out);
            }
        };

        return builder.body(body);
    }

    @PostMapping(MAPPING_PATH_DOCUMENT_CHECKIN_BY_ID)
    public ResponseEntity<DocumentReadDto> checkIn(@AuthenticationPrincipal Jwt jwt,
                                                   @PathVariable(PATH_VARIABLE_DOCUMENT_ID) String id,
                                                   @Valid @RequestBody DocumentCheckinDto dto) {
        String ownerUid = jwt.getSubject();
        DocumentReadDto updated = documentService.checkIn(ownerUid, id, dto);
        return ok(updated);
    }

    @PutMapping(MAPPING_PATH_DOCUMENT_BY_ID)
    public ResponseEntity<DocumentReadDto> update(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable(PATH_VARIABLE_DOCUMENT_ID) String id,
                                                  @Valid @RequestBody DocumentUpdateDto dto) {
        String ownerUid = jwt.getSubject();

        DocumentReadDto document = documentService.update(ownerUid, id, dto);
        return ok().body(document);
    }

    @DeleteMapping(MAPPING_PATH_DOCUMENT_BY_ID)
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable(PATH_VARIABLE_DOCUMENT_ID) String id) {
        String ownerUid = jwt.getSubject();
        documentService.delete(ownerUid, id);
        return noContent().build();
    }
}
