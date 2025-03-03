package com.example.grpcbook.readinggoal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, String> {

    // 카테고리별 독서 목표 찾기
    Page<ReadingGoal> findByCategory(String category, Pageable pageable);

    // 완료 상태별 독서 목표 찾기
    Page<ReadingGoal> findByCompleted(boolean completed, Pageable pageable);

    // 카테고리와 완료 상태별 독서 목표 찾기
    Page<ReadingGoal> findByCategoryAndCompleted(String category, boolean completed, Pageable pageable);

    // 특정 도서에 대한 독서 목표 찾기
    List<ReadingGoal> findByBookId(String bookId);
}
