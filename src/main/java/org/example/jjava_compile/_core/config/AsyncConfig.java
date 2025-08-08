package org.example.jjava_compile._core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 작업 처리를 위한 스레드풀 설정 클래스
 * <p>
 * Spring의 @Async 어노테이션이 붙은 메서드를 호출하면
 * 별도의 스레드풀에서 비동기적으로 실행되는데,
 * 그 스레드풀을 커스터마이징하기 위해 이 설정을 사용함.
 */
@Configuration
@EnableAsync  // @Async 비동기 실행 기능 활성화
public class AsyncConfig {


    /**
     * 'compileExecutor'라는 이름의 스레드풀 빈(Bean)을 생성
     * 주로 코드 컴파일이나 실행 같은 CPU 연산 작업을
     * 요청마다 별도 스레드에서 처리할 때 사용.
     * <p>
     * Executor : 비동기 작업 실행용 스레드풀
     */
    @Bean(name = "compileExecutor")
    public Executor compileExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);  // 동시에 실행 가능한 기본 스레드 수
        executor.setMaxPoolSize(10);  // 최대 스레드 수 (큐가 가득 찼을 때 확장 가능한 스레드 수)
        executor.setQueueCapacity(20);  // 작업 대기 큐 용량 (CorePoolSize 개 이상의 요청이 들어올 때 큐에 저장)
        executor.initialize();  // 스레드풀 초기화
        return executor;
    }

}
