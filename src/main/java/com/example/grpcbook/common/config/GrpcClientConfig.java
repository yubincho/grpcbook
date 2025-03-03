package com.example.grpcbook.common.config;

import com.example.grpcbook.generated.BookServiceGrpc;
import com.example.grpcbook.generated.ReadingGoalServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GrpcClientConfig {

    @Value("${grpc.client.server-address:localhost}")
    private String serverAddress;

    @Value("${grpc.client.server-port:50051}")
    private int serverPort;

    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();
    }

    @Bean
    public BookServiceGrpc.BookServiceBlockingStub bookServiceBlockingStub(ManagedChannel managedChannel) {
        return BookServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public BookServiceGrpc.BookServiceStub bookServiceStub(ManagedChannel managedChannel) {
        return BookServiceGrpc.newStub(managedChannel);
    }

    @Bean
    public ReadingGoalServiceGrpc.ReadingGoalServiceBlockingStub readingGoalServiceBlockingStub(ManagedChannel managedChannel) {
        return ReadingGoalServiceGrpc.newBlockingStub(managedChannel);
    }

    @Bean
    public ReadingGoalServiceGrpc.ReadingGoalServiceStub readingGoalServiceStub(ManagedChannel managedChannel) {
        return ReadingGoalServiceGrpc.newStub(managedChannel);
    }
}
