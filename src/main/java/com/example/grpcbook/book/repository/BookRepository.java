package com.example.grpcbook.book.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface BookRepository extends JpaRepository<Book, String> {

    // 카테고리별 도서 찾기
    Page<Book> findByCategory(String category, Pageable pageable);

    // 제목이나 저자로 도서 검색
    List<Book> findByTitleContainingOrAuthorContaining(String title, String author);
}
