package com.musicquiz.controller;

import com.musicquiz.dto.QuizAnswerRequest;
import com.musicquiz.dto.QuizQuestionResponse;
import com.musicquiz.dto.QuizSessionResponse;
import com.musicquiz.dto.QuizStartRequest;
import com.musicquiz.model.QuizQuestion;
import com.musicquiz.model.QuizSession;
import com.musicquiz.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * Начать новую сессию викторины.
     * POST /api/quiz/start
     * Body: { "numberOfQuestions": 5 }
     */
    @PostMapping("/start")
    public ResponseEntity<QuizSessionResponse> startQuiz(
            @Valid @RequestBody QuizStartRequest request) {

        QuizSession session = quizService.startSession(request.getNumberOfQuestions());
        return ResponseEntity.ok(QuizSessionResponse.from(session));
    }

    /**
     * Получить вопрос по номеру.
     * GET /api/quiz/{sessionId}/question/{questionNumber}
     * Аудио для вопроса: GET /api/songs/{songId}/audio
     */
    @GetMapping("/{sessionId}/question/{questionNumber}")
    public ResponseEntity<QuizQuestionResponse> getQuestion(
            @PathVariable Long sessionId,
            @PathVariable int questionNumber) {

        QuizQuestion question = quizService.getQuestion(sessionId, questionNumber);
        int total = quizService.getTotalQuestions(sessionId);
        return ResponseEntity.ok(QuizQuestionResponse.from(question, total));
    }

    /**
     * Ответить на вопрос.
     * POST /api/quiz/{sessionId}/answer/{questionNumber}
     * Body: { "answer": "Название песни" }
     * Ответ содержит правильный ответ и результат.
     */
    @PostMapping("/{sessionId}/answer/{questionNumber}")
    public ResponseEntity<QuizQuestionResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable int questionNumber,
            @Valid @RequestBody QuizAnswerRequest request) {

        QuizQuestion question = quizService.submitAnswer(sessionId, questionNumber, request.getAnswer());
        int total = quizService.getTotalQuestions(sessionId);
        return ResponseEntity.ok(QuizQuestionResponse.from(question, total));
    }

    /**
     * Завершить сессию.
     * POST /api/quiz/{sessionId}/finish
     */
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<QuizSessionResponse> finishQuiz(@PathVariable Long sessionId) {
        QuizSession session = quizService.finishSession(sessionId);
        return ResponseEntity.ok(QuizSessionResponse.from(session));
    }

    /**
     * История всех сессий.
     * GET /api/quiz/history
     */
    @GetMapping("/history")
    public ResponseEntity<List<QuizSessionResponse>> getHistory() {
        List<QuizSessionResponse> history = quizService.getHistory().stream()
                .map(QuizSessionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Детали сессии.
     * GET /api/quiz/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<QuizSessionResponse> getSession(@PathVariable Long sessionId) {
        return ResponseEntity.ok(QuizSessionResponse.from(quizService.getSession(sessionId)));
    }
}
