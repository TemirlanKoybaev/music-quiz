package com.musicquiz.musicquiz;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
}