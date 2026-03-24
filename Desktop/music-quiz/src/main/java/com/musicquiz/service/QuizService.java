package com.musicquiz.service;

import com.musicquiz.model.QuizQuestion;
import com.musicquiz.model.QuizSession;
import com.musicquiz.model.Song;
import com.musicquiz.repository.QuizQuestionRepository;
import com.musicquiz.repository.QuizSessionRepository;
import com.musicquiz.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizSessionRepository sessionRepository;
    private final QuizQuestionRepository questionRepository;
    private final SongRepository songRepository;

    @Value("${app.quiz.options-per-question}")
    private int optionsPerQuestion;

    @Transactional
    public QuizSession startSession(int numberOfQuestions) {
        List<Song> allSongs = songRepository.findAll();
        if (allSongs.size() < optionsPerQuestion) {
            throw new IllegalStateException(
                "Недостаточно песен для викторины. Нужно минимум " + optionsPerQuestion +
                ", загружено: " + allSongs.size()
            );
        }
        if (allSongs.size() < numberOfQuestions) {
            throw new IllegalStateException(
                "Недостаточно песен. Запрошено " + numberOfQuestions +
                " вопросов, но загружено только " + allSongs.size()
            );
        }

        QuizSession session = QuizSession.builder()
                .totalQuestions(numberOfQuestions)
                .score(0)
                .build();
        session = sessionRepository.save(session);

        List<Song> shuffled = new ArrayList<>(allSongs);
        Collections.shuffle(shuffled);
        List<Song> selectedSongs = shuffled.subList(0, numberOfQuestions);

        List<QuizQuestion> questions = new ArrayList<>();
        for (int i = 0; i < selectedSongs.size(); i++) {
            Song correct = selectedSongs.get(i);
            List<String> options = buildOptions(correct, allSongs);

            QuizQuestion question = QuizQuestion.builder()
                    .session(session)
                    .correctSong(correct)
                    .options(options)
                    .questionOrder(i + 1)
                    .build();
            questions.add(question);
        }
        questionRepository.saveAll(questions);

        return session;
    }

    public QuizQuestion getQuestion(Long sessionId, int questionOrder) {
        QuizSession session = getActiveSession(sessionId);
        if (questionOrder < 1 || questionOrder > session.getTotalQuestions()) {
            throw new IllegalArgumentException("Номер вопроса вне диапазона: " + questionOrder);
        }
        return questionRepository.findBySessionIdAndQuestionOrder(sessionId, questionOrder)
                .orElseThrow(() -> new IllegalArgumentException("Вопрос не найден"));
    }

    @Transactional
    public QuizQuestion submitAnswer(Long sessionId, int questionOrder, String answer) {
        QuizSession session = getActiveSession(sessionId);
        QuizQuestion question = getQuestion(sessionId, questionOrder);

        if (question.getUserAnswer() != null) {
            throw new IllegalStateException("На этот вопрос уже дан ответ");
        }

        boolean isCorrect = question.getCorrectSong().getTitle().equalsIgnoreCase(answer.trim());
        question.setUserAnswer(answer.trim());
        question.setCorrect(isCorrect);
        question.setAnsweredAt(LocalDateTime.now());
        questionRepository.save(question);

        if (isCorrect) {
            session.setScore(session.getScore() + 1);
            sessionRepository.save(session);
        }

        return question;
    }

    @Transactional
    public QuizSession finishSession(Long sessionId) {
        QuizSession session = getActiveSession(sessionId);
        session.setStatus(QuizSession.SessionStatus.COMPLETED);
        session.setFinishedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    public List<QuizSession> getHistory() {
        return sessionRepository.findAllByOrderByStartedAtDesc();
    }

    public QuizSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Сессия не найдена: " + sessionId));
    }

    public int getTotalQuestions(Long sessionId) {
        return getSession(sessionId).getTotalQuestions();
    }

    // --- private helpers ---

    private QuizSession getActiveSession(Long sessionId) {
        QuizSession session = getSession(sessionId);
        if (session.getStatus() != QuizSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Сессия уже завершена (статус: " + session.getStatus() + ")");
        }
        return session;
    }

    private List<String> buildOptions(Song correct, List<Song> allSongs) {
        List<String> wrongTitles = allSongs.stream()
                .filter(s -> !s.getId().equals(correct.getId()))
                .map(Song::getTitle)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(wrongTitles);

        List<String> options = new ArrayList<>();
        options.add(correct.getTitle());
        options.addAll(wrongTitles.subList(0, Math.min(optionsPerQuestion - 1, wrongTitles.size())));
        Collections.shuffle(options);
        return options;
    }
}
