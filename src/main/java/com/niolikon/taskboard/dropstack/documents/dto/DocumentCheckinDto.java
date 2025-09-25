package com.niolikon.taskboard.dropstack.documents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentCheckinDto {
    @NotBlank
    private String categoryCode;

    @NotNull
    private Long version;
}
