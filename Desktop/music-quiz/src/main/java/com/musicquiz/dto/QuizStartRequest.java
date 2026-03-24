package com.musicquiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuizStartRequest {

    @Min(value = 1, message = "Минимум 1 вопрос")
    @Max(value = 20, message = "Максимум 20 вопросов")
    private int numberOfQuestions = 5;
}
