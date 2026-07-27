package com.wd.ms_enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectionRequest {

    @NotBlank(message = "La justificación de rechazo es obligatoria.")
    @Size(max = 255, message = "La justificación no puede superar los 255 caracteres.")
    private String reason;
}