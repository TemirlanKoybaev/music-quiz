package com.musicquiz.dto;

import com.musicquiz.model.QuizQuestion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QuizQuestionResponse {

    private Long id;
    private int questionOrder;
    private int totalQuestions;
    private Long songId;
    private List<String> options;

    // Поля заполняются только после ответа
    private String userAnswer;
    private Boolean correct;
    private String correctAnswer;
    private LocalDateTime answeredAt;

    /** Вопрос без раскрытия правильного ответа */
    public static QuizQuestionResponse from(QuizQuestion q, int totalQuestions) {
        return QuizQuestionResponse.builder()
                .id(q.getId())
                .questionOrder(q.getQuestionOrder())
                .totalQuestions(totalQuestions)
                .songId(q.getCorrectSong().getId())
                .options(q.getOptions())
                .userAnswer(q.getUserAnswer())
                .correct(q.getUserAnswer() != null ? q.isCorrect() : null)
                .correctAnswer(q.getUserAnswer() != null ? q.getCorrectSong().getTitle() : null)
                .answeredAt(q.getAnsweredAt())
                .build();
    }
}
