//package com.example.grpcbook.common;
//
//
//
//@Component
//public class DataInitializer implements CommandLineRunner {
//
//    @Autowired
//    private BookRepository bookRepository;
//
//    @Autowired
//    private ReadingGoalRepository readingGoalRepository;
//
//    @Override
//    public void run(String... args) throws Exception {
//        // 기존 데이터 삭제
//        bookRepository.deleteAll();
//        readingGoalRepository.deleteAll();
//
//        // 샘플 도서 데이터 생성
//        List<Book> books = Arrays.asList(
//                new Book("자바의 정석", "남궁성", "프로그래밍"),
//                new Book("클린 코드", "로버트 C. 마틴", "프로그래밍"),
//                new Book("effective java", "조슈아 블로크", "프로그래밍"),
//                new Book("해리 포터와 마법사의 돌", "J.K. 롤링", "소설"),
//                new Book("1984", "조지 오웰", "소설"),
//                new Book("파이썬을 이용한 머신러닝", "오렐리앙 제롱", "데이터 과학")
//        );
//
//        // 도서 저장
//        books = bookRepository.saveAll(books);
//
//        // 샘플 독서 목표 생성
//        List<ReadingGoal> goals = Arrays.asList(
//                new ReadingGoal(
//                        books.get(0).getId(),
//                        "자바 마스터하기",
//                        "자바 기본 문법과 객체지향 프로그래밍 익히기",
//                        "학습",
//                        LocalDate.now().plusMonths(1)
//                ),
//                new ReadingGoal(
//                        books.get(3).getId(),
//                        "해리포터 시리즈 읽기",
//                        "유명한 판타지 소설 시리즈 읽기",
//                        "취미",
//                        LocalDate.now().plusMonths(2)
//                )
//        );
//
//        // 독서 목표 저장
//        readingGoalRepository.saveAll(goals);
//
//        System.out.println("샘플 데이터가 성공적으로 로드되었습니다.");
//    }
//}
