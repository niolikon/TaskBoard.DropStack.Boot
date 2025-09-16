package com.niolikon.taskboard.dropstack.documents.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentUpdateDto {

    @JsonProperty("Title")
    @NotBlank
    @Size(max = 200)
    private String title;

    @JsonProperty("Tags")
    @NotNull
    private List<@NotBlank @Size(max = 64) String> tags;

    @JsonProperty("Version")
    @NotNull
    private Long version;
}
