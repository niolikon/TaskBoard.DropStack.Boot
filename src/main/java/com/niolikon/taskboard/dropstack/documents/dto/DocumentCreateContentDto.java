package com.niolikon.taskboard.dropstack.documents.dto;

import lombok.*;
import org.springframework.core.io.InputStreamSource;
import jakarta.validation.constraints.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentCreateContentDto {

    @NotNull
    private InputStreamSource source;

    @Positive
    private Long size;

    @NotEmpty
    private String originalFilename;
}
