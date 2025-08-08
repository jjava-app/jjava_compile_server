package org.example.jjava_compile.compile;


import lombok.RequiredArgsConstructor;
import org.example.jjava_compile._util.ErrorMessageUtil;
import org.example.jjava_compile._util.Resp;
import org.example.jjava_compile._util.Util;
import org.example.jjava_compile.dto.CompileRequest;
import org.example.jjava_compile.dto.CompileResponse;
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

        // 2. 유효성 검사
        if (reqDTO.getPayload() == null || reqDTO.getPayload().isBlank()) {
            return Resp.ok(new CompileResponse.DTO(null, "코드가 비어 있습니다."));
        }

        try {
            // 3. 비동기 실행 (전용 스레드풀에서 실행)
            CompletableFuture<CompileResponse.DTO> future = CompletableFuture.supplyAsync(() -> {
                String output = Util.executeJs(reqDTO.getPayload());
                return new CompileResponse.DTO(reqDTO.getPayload(), output);
            }, compileExecutor);

            // 4. 최대 2초 대기 (초과 시 TimeoutException) -무한루프 방지
            CompileResponse.DTO respDTO = future.get(2, TimeUnit.SECONDS);
            return Resp.ok(respDTO);

        } catch (TimeoutException e) {
            // 5. 시간 초과 응답
            return Resp.ok(new CompileResponse.DTO(null, "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)"));
        } catch (Exception e) {
            // 6. 그 외 예외 처리 (메시지 가공 후 응답)
            String msg = ErrorMessageUtil.errorMessage(Optional.ofNullable(e.getMessage()).orElse(""));
            return Resp.ok(new CompileResponse.DTO(null, msg));
        }
    }
}
