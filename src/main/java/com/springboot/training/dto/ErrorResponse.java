package com.springboot.training.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<FieldViolation> fieldErrors) {
}