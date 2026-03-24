package com.musicquiz.repository;

import com.musicquiz.model.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

    List<QuizQuestion> findBySessionIdOrderByQuestionOrder(Long sessionId);

    Optional<QuizQuestion> findBySessionIdAndQuestionOrder(Long sessionId, int questionOrder);
}
