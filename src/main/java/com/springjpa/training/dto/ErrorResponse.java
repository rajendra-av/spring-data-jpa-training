package com.springjpa.training.dto;

import java.util.List;

public record ErrorResponse(int status, String message, List<FieldViolation> fieldErrors) {
}