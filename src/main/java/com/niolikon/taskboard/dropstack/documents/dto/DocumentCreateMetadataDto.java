package com.niolikon.taskboard.dropstack.documents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentCreateMetadataDto {

    @JsonProperty("Title")
    @NotBlank
    @Size(max = 200)
    private String title;

    @JsonProperty("MimeType")
    @NotBlank
    @Pattern(regexp = "^[\\w.+-]+/[\\w.+-]+$", message = "MimeType not valid")
    private String mimeType;

    @JsonProperty("Tags")
    @Builder.Default
    private List<@NotBlank @Size(max = 64) String> tags = java.util.List.of();
}
