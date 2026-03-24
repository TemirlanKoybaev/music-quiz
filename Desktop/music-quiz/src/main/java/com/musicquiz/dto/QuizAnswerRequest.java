package com.musicquiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizAnswerRequest {

    @NotBlank(message = "Ответ не может быть пустым")
    private String answer;
}
