package org.example.jjava_compile.compile;


import lombok.RequiredArgsConstructor;
import org.example.jjava_compile._util.ErrorMessageUtil;
import org.example.jjava_compile._util.Resp;
import org.example.jjava_compile.dto.CompileRequest;
import org.example.jjava_compile.dto.CompileResponse;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@RestController
public class CompileController {


    private final Executor compileExecutor; // 비동기 실행 전용 스레드풀 주입 (AsyncConfig에서 정의)

    @PostMapping("/compile")
    public ResponseEntity<?> jsRun(@RequestBody CompileRequest.DTO reqDTO) {

        // 1. 지원 언어 검사
        if (!"javascript".equalsIgnoreCase(reqDTO.getType())) {
            return Resp.ok(new CompileResponse.DTO(null, "지원하지 않는 코드 유형입니다."));
        }

        try {
            // 2. 비동기 실행 (전용 스레드풀에서 실행)
            CompletableFuture<CompileResponse.DTO> future = CompletableFuture.supplyAsync(() -> {
                try (Context context = Context.newBuilder("js")  // JavaScript 실행 컨텍스트 생성
                        .allowAllAccess(true)  // 모든 접근 허용 (console, window 등)
                        .build()) {

                    // 3. JS 코드 실행 전, console.log / window.alert 오버라이드
                    context.eval("js", """
                                var outputLogs = [];
                                var console = { log: function(msg) { outputLogs.push(String(msg)); } };
                                var window = { alert: function(msg) { outputLogs.push(String(msg)); } };
                            """);

                    // 4. 실제 사용자 코드 실행
                    context.eval("js", reqDTO.getPayload());


                    // 5. JS 코드 내에서 출력된 로그 수집
                    Value logs = context.getBindings("js").getMember("outputLogs");
                    StringBuilder sb = new StringBuilder();
                    if (logs != null && logs.hasArrayElements()) {
                        for (int i = 0; i < logs.getArraySize(); i++) {
                            sb.append(logs.getArrayElement(i).asString()).append("\n");
                        }
                    }

                    // 6. 실행 코드와 결과 문자열을 DTO로 반환
                    return new CompileResponse.DTO(reqDTO.getPayload(), sb.toString().trim());
                }
            }, compileExecutor); // 전용 스레드풀 사용

            // 7. 최대 2초 대기 (초과 시 TimeoutException) -무한루프 방지
            CompileResponse.DTO respDTO = future.get(2, TimeUnit.SECONDS);
            return Resp.ok(respDTO);

        } catch (TimeoutException e) {
            // 9. 시간 초과 응답
            return Resp.ok(new CompileResponse.DTO(null, "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)"));
        } catch (Exception e) {
            // 10. 그 외 예외 처리 (메시지 가공 후 응답)
            String msg = ErrorMessageUtil.errorMessage(Optional.ofNullable(e.getMessage()).orElse(""));
            return Resp.ok(new CompileResponse.DTO(null, msg));
        }
    }
}
