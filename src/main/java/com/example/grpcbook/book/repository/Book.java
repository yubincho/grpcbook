package com.example.grpcbook.book.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Entity
@Table(name = "books")
public class Book {

    @Id @Setter
    private String id;

    @Setter
    private String title;
    @Setter
    private String author;
    @Setter
    private String category;
    @Setter
    private boolean available;

    public Book() {
        this.id = UUID.randomUUID().toString();
        this.available = true;
    }

    public Book(String title, String author, String category) {
        this();
        this.title = title;
        this.author = author;
        this.category = category;
    }

    // JPA 엔티티에서 gRPC 메시지로 변환
    public com.example.grpcbook.generated.Book toGrpcBook() {
        return com.example.grpcbook.generated.Book.newBuilder()
                .setId(this.id)
                .setTitle(this.title)
                .setAuthor(this.author)
                .setCategory(this.category)
                .setAvailable(this.available)
                .build();
    }
}
