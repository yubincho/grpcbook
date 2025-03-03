package com.example.grpcbook.book.service;

import com.example.grpcbook.generated.Book;
import com.example.grpcbook.generated.BookResponse;
import com.example.grpcbook.generated.BookServiceGrpc;
import com.example.grpcbook.generated.BorrowBookRequest;
import com.example.grpcbook.generated.CreateBookRequest;
import com.example.grpcbook.generated.DeleteBookRequest;
import com.example.grpcbook.generated.DeleteBookResponse;
import com.example.grpcbook.generated.GetBookRequest;
import com.example.grpcbook.generated.ListBooksRequest;
import com.example.grpcbook.generated.ListBooksResponse;
import com.example.grpcbook.generated.ReturnBookRequest;
import com.example.grpcbook.generated.UpdateBookRequest;
import com.example.grpcbook.generated.WatchBookRequest;

import com.example.grpcbook.book.repository.BookRepository;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@GrpcService
public class BookServiceImpl extends BookServiceGrpc.BookServiceImplBase {

    @Autowired
    private BookRepository bookRepository;

    // 스트리밍 클라이언트 관리를 위한 맵
    private final ConcurrentMap<String, ConcurrentMap<StreamObserver<BookResponse>, Boolean>> bookWatchers = new ConcurrentHashMap<>();

    @Override
    public void createBook(CreateBookRequest request, StreamObserver<BookResponse> responseObserver) {
        com.example.grpcbook.book.repository.Book book = new com.example.grpcbook.book.repository.Book(
                request.getTitle(),
                request.getAuthor(),
                request.getCategory()
        );

        book = bookRepository.save(book);

        BookResponse response = BookResponse.newBuilder()
                .setBook(book.toGrpcBook())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // 책이 생성되면 이 책을 지켜보고 있는 관찰자들에게 알림
        notifyBookWatchers(book.getId(), response);
    }

    @Override
    public void getBook(GetBookRequest request, StreamObserver<BookResponse> responseObserver) {
        String bookId = request.getId();
        Optional<com.example.grpcbook.book.repository.Book> optionalBook = bookRepository.findById(bookId);

        if (optionalBook.isPresent()) {
            com.example.grpcbook.book.repository.Book book = optionalBook.get();
            BookResponse response = BookResponse.newBuilder()
                    .setBook(book.toGrpcBook())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
        }
    }

    @Override
    public void listBooks(ListBooksRequest request, StreamObserver<ListBooksResponse> responseObserver) {
        int page = request.getPage();
        int size = request.getSize();

        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.grpcbook.book.repository.Book> bookPage;

        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            bookPage = bookRepository.findByCategory(request.getCategory(), pageable);
        } else {
            bookPage = bookRepository.findAll(pageable);
        }

        ListBooksResponse.Builder responseBuilder = ListBooksResponse.newBuilder();

        bookPage.getContent().forEach(book -> {
            responseBuilder.addBooks(book.toGrpcBook());
        });

        responseBuilder.setTotalCount((int) bookPage.getTotalElements());

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateBook(UpdateBookRequest request, StreamObserver<BookResponse> responseObserver) {
        String bookId = request.getId();
        Optional<com.example.grpcbook.book.repository.Book> optionalBook = bookRepository.findById(bookId);

        if (optionalBook.isPresent()) {
            com.example.grpcbook.book.repository.Book book = optionalBook.get();

            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                book.setTitle(request.getTitle());
            }

            if (request.getAuthor() != null && !request.getAuthor().isEmpty()) {
                book.setAuthor(request.getAuthor());
            }

            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                book.setCategory(request.getCategory());
            }

            book = bookRepository.save(book);

            BookResponse response = BookResponse.newBuilder()
                    .setBook(book.toGrpcBook())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // 책이 업데이트되면 관찰자들에게 알림
            notifyBookWatchers(bookId, response);
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
        }
    }

    @Override
    public void deleteBook(DeleteBookRequest request, StreamObserver<DeleteBookResponse> responseObserver) {
        String bookId = request.getId();

        if (bookRepository.existsById(bookId)) {
            bookRepository.deleteById(bookId);

            DeleteBookResponse response = DeleteBookResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // 책이 삭제되면 관찰자들에게 알림 후 관찰자 목록 제거
            if (bookWatchers.containsKey(bookId)) {
                bookWatchers.remove(bookId);
            }
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
        }
    }

    @Override
    public void borrowBook(BorrowBookRequest request, StreamObserver<BookResponse> responseObserver) {
        String bookId = request.getId();
        Optional<com.example.grpcbook.book.repository.Book> optionalBook = bookRepository.findById(bookId);

        if (optionalBook.isPresent()) {
            com.example.grpcbook.book.repository.Book book = optionalBook.get();

            if (book.isAvailable()) {
                book.setAvailable(false);
                book = bookRepository.save(book);

                BookResponse response = BookResponse.newBuilder()
                        .setBook(book.toGrpcBook())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

                // 책 상태가 변경되면 관찰자들에게 알림
                notifyBookWatchers(bookId, response);
            } else {
                responseObserver.onError(
                        new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("이미 대여 중인 도서입니다: " + bookId))
                );
            }
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
        }
    }

    @Override
    public void returnBook(ReturnBookRequest request, StreamObserver<BookResponse> responseObserver) {
        String bookId = request.getId();
        Optional<com.example.grpcbook.book.repository.Book> optionalBook = bookRepository.findById(bookId);

        if (optionalBook.isPresent()) {
            com.example.grpcbook.book.repository.Book book = optionalBook.get();

            if (!book.isAvailable()) {
                book.setAvailable(true);
                book = bookRepository.save(book);

                BookResponse response = BookResponse.newBuilder()
                        .setBook(book.toGrpcBook())
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();

                // 책 상태가 변경되면 관찰자들에게 알림
                notifyBookWatchers(bookId, response);
            } else {
                responseObserver.onError(
                        new StatusRuntimeException(Status.FAILED_PRECONDITION.withDescription("이미 반납된 도서입니다: " + bookId))
                );
            }
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
        }
    }

    @Override
    public void watchBookStatus(WatchBookRequest request, StreamObserver<BookResponse> responseObserver) {
        String bookId = request.getId();

        // 관찰자 등록
        bookWatchers.computeIfAbsent(bookId, k -> new ConcurrentHashMap<>()).put(responseObserver, Boolean.TRUE);

        // 현재 책 상태 전송
        Optional<com.example.grpcbook.book.repository.Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            com.example.grpcbook.book.repository.Book book = optionalBook.get();
            BookResponse response = BookResponse.newBuilder()
                    .setBook(book.toGrpcBook())
                    .build();

            responseObserver.onNext(response);
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("도서를 찾을 수 없습니다: " + bookId))
            );
            // 오류 발생 시 관찰자 목록에서 제거
            if (bookWatchers.containsKey(bookId)) {
                bookWatchers.get(bookId).remove(responseObserver);
            }
        }
    }

    // 특정 도서의 모든 관찰자에게 알림
    private void notifyBookWatchers(String bookId, BookResponse response) {
        if (bookWatchers.containsKey(bookId)) {
            bookWatchers.get(bookId).forEach((observer, aBoolean) -> {
                try {
                    observer.onNext(response);
                } catch (Exception e) {
                    // 오류 발생 시 관찰자 목록에서 제거
                    bookWatchers.get(bookId).remove(observer);
                }
            });
        }
    }
}