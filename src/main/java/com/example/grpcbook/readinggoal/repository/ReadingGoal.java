package com.example.grpcbook.readinggoal.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;


@Setter
@Getter
@Entity
@Table(name = "reading_goals")
public class ReadingGoal {

    @Id
    private String id;

    private String bookId;
    private String title;
    private String description;
    private boolean completed;
    private String category;
    private LocalDate deadline;

    public ReadingGoal() {
        this.id = UUID.randomUUID().toString();
        this.completed = false;
    }

    public ReadingGoal(String bookId, String title, String description, String category, LocalDate deadline) {
        this();
        this.bookId = bookId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
    }

    // JPA 엔티티에서 gRPC 메시지로 변환
    public com.example.grpcbook.generated.ReadingGoal toGrpcReadingGoal() {
        return com.example.grpcbook.generated.ReadingGoal.newBuilder()
                .setId(this.id)
                .setBookId(this.bookId)
                .setTitle(this.title)
                .setDescription(this.description)
                .setCompleted(this.completed)
                .setCategory(this.category)
                .setDeadline(this.deadline.toString())
                .build();
    }
}
