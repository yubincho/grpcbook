package com.example.grpcbook.readinggoal.service;

// JPA 엔티티 클래스 import
import com.example.grpcbook.readinggoal.repository.ReadingGoal;
import com.example.grpcbook.readinggoal.repository.ReadingGoalRepository;

// gRPC 클래스들은 전체 경로 사용 (import 하지 않음)
// import com.example.grpcbook.generated.ReadingGoal;
import com.example.grpcbook.generated.ReadingGoalServiceGrpc;
import com.example.grpcbook.generated.CreateGoalRequest;
import com.example.grpcbook.generated.GetGoalRequest;
import com.example.grpcbook.generated.ListGoalsRequest;
import com.example.grpcbook.generated.ListGoalsResponse;
import com.example.grpcbook.generated.UpdateGoalRequest;
import com.example.grpcbook.generated.DeleteGoalRequest;
import com.example.grpcbook.generated.DeleteGoalResponse;
import com.example.grpcbook.generated.CompleteGoalRequest;
import com.example.grpcbook.generated.GoalResponse;
import com.example.grpcbook.generated.GoalUpdateRequest;
import com.example.grpcbook.generated.GoalUpdateResponse;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@GrpcService
public class ReadingGoalServiceImpl extends ReadingGoalServiceGrpc.ReadingGoalServiceImplBase {

    @Autowired
    private ReadingGoalRepository readingGoalRepository;

    // 양방향 스트리밍 세션 관리
    private final ConcurrentMap<StreamObserver<GoalUpdateResponse>, StreamObserver<GoalUpdateRequest>> streamingSessions = new ConcurrentHashMap<>();

    @Override
    public void createGoal(CreateGoalRequest request, StreamObserver<GoalResponse> responseObserver) {
        // JPA 엔티티 생성 - 명시적 타입 지정
        ReadingGoal entityGoal = new ReadingGoal(
                request.getBookId(),
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                LocalDate.parse(request.getDeadline())
        );

        entityGoal = readingGoalRepository.save(entityGoal);

        // ID를 별도 변수에 저장
        String goalId = entityGoal.getId();

        // gRPC 메시지로 변환 - 항상 전체 경로 사용
        com.example.grpcbook.generated.ReadingGoal grpcGoal = entityGoal.toGrpcReadingGoal();

        GoalResponse response = GoalResponse.newBuilder()
                .setGoal(grpcGoal)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        // 스트리밍 세션에 업데이트 알림
        notifyGoalUpdate(goalId, "create", grpcGoal);
    }

    @Override
    public void getGoal(GetGoalRequest request, StreamObserver<GoalResponse> responseObserver) {
        String goalId = request.getId();
        Optional<com.example.grpcbook.readinggoal.repository.ReadingGoal> optionalGoal = readingGoalRepository.findById(goalId);

        if (optionalGoal.isPresent()) {
            com.example.grpcbook.readinggoal.repository.ReadingGoal goal = optionalGoal.get();
            GoalResponse response = GoalResponse.newBuilder()
                    .setGoal(goal.toGrpcReadingGoal())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("독서 목표를 찾을 수 없습니다: " + goalId))
            );
        }
    }

    @Override
    public void listGoals(ListGoalsRequest request, StreamObserver<ListGoalsResponse> responseObserver) {
        int page = request.getPage();
        int size = request.getSize();

        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.grpcbook.readinggoal.repository.ReadingGoal> goalPage;

        String category = request.getCategory();
        boolean hasCategory = category != null && !category.isEmpty();

        if (hasCategory && request.hasCompleted()) {
            goalPage = readingGoalRepository.findByCategoryAndCompleted(category, request.getCompleted(), pageable);
        } else if (hasCategory) {
            goalPage = readingGoalRepository.findByCategory(category, pageable);
        } else if (request.hasCompleted()) {
            goalPage = readingGoalRepository.findByCompleted(request.getCompleted(), pageable);
        } else {
            goalPage = readingGoalRepository.findAll(pageable);
        }

        ListGoalsResponse.Builder responseBuilder = ListGoalsResponse.newBuilder();

        goalPage.getContent().forEach(goal -> {
            responseBuilder.addGoals(goal.toGrpcReadingGoal());
        });

        responseBuilder.setTotalCount((int) goalPage.getTotalElements());

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateGoal(UpdateGoalRequest request, StreamObserver<GoalResponse> responseObserver) {
        String goalId = request.getId();
        Optional<com.example.grpcbook.readinggoal.repository.ReadingGoal> optionalGoal = readingGoalRepository.findById(goalId);

        if (optionalGoal.isPresent()) {
            com.example.grpcbook.readinggoal.repository.ReadingGoal goal = optionalGoal.get();

            if (request.getTitle() != null && !request.getTitle().isEmpty()) {
                goal.setTitle(request.getTitle());
            }

            if (request.getDescription() != null && !request.getDescription().isEmpty()) {
                goal.setDescription(request.getDescription());
            }

            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                goal.setCategory(request.getCategory());
            }

            if (request.getDeadline() != null && !request.getDeadline().isEmpty()) {
                goal.setDeadline(LocalDate.parse(request.getDeadline()));
            }

            goal = readingGoalRepository.save(goal);

            GoalResponse response = GoalResponse.newBuilder()
                    .setGoal(goal.toGrpcReadingGoal())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // 스트리밍 세션에 업데이트 알림
            notifyGoalUpdate(goalId, "update", goal.toGrpcReadingGoal());
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("독서 목표를 찾을 수 없습니다: " + goalId))
            );
        }
    }

    @Override
    public void deleteGoal(DeleteGoalRequest request, StreamObserver<DeleteGoalResponse> responseObserver) {
        String goalId = request.getId();

        if (readingGoalRepository.existsById(goalId)) {
            readingGoalRepository.deleteById(goalId);

            DeleteGoalResponse response = DeleteGoalResponse.newBuilder()
                    .setSuccess(true)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // 스트리밍 세션에 삭제 알림
            notifyGoalUpdate(goalId, "delete", null);
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("독서 목표를 찾을 수 없습니다: " + goalId))
            );
        }
    }

    @Override
    public void completeGoal(CompleteGoalRequest request, StreamObserver<GoalResponse> responseObserver) {
        String goalId = request.getId();
        Optional<com.example.grpcbook.readinggoal.repository.ReadingGoal> optionalGoal = readingGoalRepository.findById(goalId);

        if (optionalGoal.isPresent()) {
            com.example.grpcbook.readinggoal.repository.ReadingGoal goal = optionalGoal.get();
            goal.setCompleted(request.getCompleted());

            goal = readingGoalRepository.save(goal);

            GoalResponse response = GoalResponse.newBuilder()
                    .setGoal(goal.toGrpcReadingGoal())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            // 스트리밍 세션에 업데이트 알림
            notifyGoalUpdate(goalId, "complete", goal.toGrpcReadingGoal());
        } else {
            responseObserver.onError(
                    new StatusRuntimeException(Status.NOT_FOUND.withDescription("독서 목표를 찾을 수 없습니다: " + goalId))
            );
        }
    }

    @Override
    public StreamObserver<GoalUpdateRequest> streamGoalUpdates(StreamObserver<GoalUpdateResponse> responseObserver) {
        // 새로운 요청 스트림 처리기 생성
        StreamObserver<GoalUpdateRequest> requestObserver = new StreamObserver<GoalUpdateRequest>() {
            @Override
            public void onNext(GoalUpdateRequest request) {
                String goalId = request.getId();
                String action = request.getAction();
                String data = request.getData();

                // 목표 존재 여부 확인
                Optional<com.example.grpcbook.readinggoal.repository.ReadingGoal> optionalGoal = readingGoalRepository.findById(goalId);

                if (optionalGoal.isPresent()) {
                    com.example.grpcbook.readinggoal.repository.ReadingGoal goal = optionalGoal.get();

                    // 액션에 따른 처리
                    switch (action) {
                        case "progress":
                            // 진행 상황 업데이트 로직
                            break;
                        case "complete":
                            goal.setCompleted(true);
                            goal = readingGoalRepository.save(goal);
                            break;
                        case "note":
                            // 메모 추가 로직 (추가 기능으로 확장 가능)
                            break;
                    }

                    // 응답 전송
                    GoalUpdateResponse response = GoalUpdateResponse.newBuilder()
                            .setId(goalId)
                            .setStatus("success")
                            .setGoal(goal.toGrpcReadingGoal())
                            .build();

                    responseObserver.onNext(response);

                    // 다른 클라이언트에게도 변경 알림
                    broadcastGoalUpdate(responseObserver, response);
                } else {
                    // 목표를 찾을 수 없는 경우 오류 응답
                    GoalUpdateResponse response = GoalUpdateResponse.newBuilder()
                            .setId(goalId)
                            .setStatus("error")
                            .build();

                    responseObserver.onNext(response);
                }
            }

            @Override
            public void onError(Throwable t) {
                // 스트리밍 세션 관리에서 제거
                streamingSessions.remove(responseObserver);
            }

            @Override
            public void onCompleted() {
                // 스트리밍 세션 종료
                streamingSessions.remove(responseObserver);
                responseObserver.onCompleted();
            }
        };

        // 스트리밍 세션 관리에 추가
        streamingSessions.put(responseObserver, requestObserver);

        return requestObserver;
    }

    // 특정 목표 변경 시 모든 스트리밍 세션에 알림
    private void notifyGoalUpdate(String goalId, String action, com.example.grpcbook.generated.ReadingGoal goal) {
        GoalUpdateResponse.Builder responseBuilder = GoalUpdateResponse.newBuilder()
                .setId(goalId)
                .setStatus(action);

        if (goal != null) {
            responseBuilder.setGoal(goal);
        }

        GoalUpdateResponse response = responseBuilder.build();

        // 모든 스트리밍 세션에 알림
        streamingSessions.forEach((responseObserver, requestObserver) -> {
            try {
                responseObserver.onNext(response);
            } catch (Exception e) {
                // 오류 발생 시 스트리밍 세션 제거
                streamingSessions.remove(responseObserver);
            }
        });
    }

    // 하나의 세션에서 다른 모든 세션으로 업데이트 브로드캐스트
    private void broadcastGoalUpdate(StreamObserver<GoalUpdateResponse> sender, GoalUpdateResponse response) {
        streamingSessions.forEach((responseObserver, requestObserver) -> {
            // 발신자가 아닌 다른 세션에만 전달
            if (responseObserver != sender) {
                try {
                    responseObserver.onNext(response);
                } catch (Exception e) {
                    // 오류 발생 시 스트리밍 세션 제거
                    streamingSessions.remove(responseObserver);
                }
            }
        });
    }
}