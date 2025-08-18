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
import java.util.concurrent.*;

@RequiredArgsConstructor
@RestController
public class CompileController {


    @PostMapping("/compile")
    public ResponseEntity<?> complie(@RequestBody CompileRequest.DTO reqDTO) {

        // 1. 지원 언어 검사
        if (!"javascript".equalsIgnoreCase(reqDTO.getType())) {
            return Resp.ok(new CompileResponse.DTO(null, "지원하지 않는 코드 유형입니다."));
        }

        // 2. 코드(payload) 유효성 검사
        if (reqDTO.getPayload() == null || reqDTO.getPayload().isBlank()) {
            return Resp.ok(new CompileResponse.DTO(null, "코드가 비어 있습니다."));
        }

        // 3. 요청 단위 임시 실행기 생성 (별도 스레드에서 코드 실행)
        ExecutorService es = Executors.newSingleThreadExecutor();
        try {
            // 3-1. 코드 실행 작업을 비동기 Future로 제출
            Future<CompileResponse.DTO> future = es.submit(() -> {
                // JavaScript 실행 유틸 호출 (실제 JS 코드 실행 로직은 Util.executeJs())
                String output = Util.executeJs(reqDTO.getPayload());
                // 실행 결과와 입력 코드를 DTO로 반환
                return new CompileResponse.DTO(reqDTO.getPayload(), output);
            });

            // 4. 최대 2초 동안만 실행 대기 (무한루프 같은 문제 방지)
            CompileResponse.DTO respDTO = future.get(2, TimeUnit.SECONDS);

            // 4-1. 정상 실행 시 결과 반환
            return Resp.ok(respDTO);

        } catch (TimeoutException e) {
            // 5. 실행 시간이 너무 오래 걸린 경우 (무한루프 가능성)
            return Resp.ok(new CompileResponse.DTO(null, "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)"));
        } catch (Exception e) {
            // 6. 그 외 예외 처리 (에러 메시지 포맷팅 후 응답)
            String msg = ErrorMessageUtil.errorMessage(Optional.ofNullable(e.getMessage()).orElse(""));
            return Resp.ok(new CompileResponse.DTO(null, msg));
        } finally {
            // 7. 실행 스레드 종료 및 자원 정리
            es.shutdownNow();
        }
    }
}
