package com.salonplatform.dto;

import jakarta.validation.constraints.*;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBarberRequest {
    @NotBlank(message = "Barber name is required")
    private String name;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    private String specialization;

    @Min(value = 0, message = "Experience cannot be negative")
    private Integer experienceYears;
}
