package com.musicquiz.repository;

import com.musicquiz.model.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    List<QuizSession> findAllByOrderByStartedAtDesc();

    List<QuizSession> findByStatusOrderByStartedAtDesc(QuizSession.SessionStatus status);
}
