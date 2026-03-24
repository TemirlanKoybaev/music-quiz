package com.musicquiz.dto;

import com.musicquiz.model.QuizSession;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class QuizSessionResponse {

    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int score;
    private int totalQuestions;
    private String status;

    public static QuizSessionResponse from(QuizSession session) {
        return QuizSessionResponse.builder()
                .id(session.getId())
                .startedAt(session.getStartedAt())
                .finishedAt(session.getFinishedAt())
                .score(session.getScore())
                .totalQuestions(session.getTotalQuestions())
                .status(session.getStatus().name())
                .build();
    }
}
