package org.example.jjava_compile.compile;

import lombok.RequiredArgsConstructor;
import org.example.jjava_compile._util.ErrorMessageUtil;
import org.example.jjava_compile._util.Resp;
import org.example.jjava_compile._util.Util;
import org.example.jjava_compile.dto.CheckRequest;
import org.example.jjava_compile.dto.CheckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

@RequiredArgsConstructor
@RestController
public class CheckController {

    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody CheckRequest.DTO reqDTO) {

        // 1. 지원 언어 검사
        if (!"javascript".equalsIgnoreCase(reqDTO.getType())) {
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null, null, "지원하지 않는 코드 유형입니다.", null, null));
        }

        // 2. 코드 유효성 검사
        if (reqDTO.getPayload() == null || reqDTO.getPayload().isBlank()) {
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null,
                    null, "코드가 비어 있습니다.", null, null
            ));
        }

        // 3. 요청 단위 임시 실행기 생성 (테스트 실행을 위한 단일 스레드)
        ExecutorService es = Executors.newSingleThreadExecutor();


        try {
            // 4. Future 비동기 실행 (모든 테스트 케이스 검증)
            Future<Object> future = es.submit(() -> {

                final List<CheckRequest.DTO.TestSpecDTO> tests = reqDTO.getTests();
                List<CheckResponse.FailDTO> failures = new ArrayList<>();
                int i = 0;  // 테스트 케이스 인덱스

                for (CheckRequest.DTO.TestSpecDTO t : tests) {

                    // (A) 바인딩(prelude) 생성
                    // - testVariable을 JS 변수 선언문으로 변환하거나, prelude 값 직접 사용
                    String prelude = (t.getPrelude() != null && !t.getPrelude().isBlank())
                            ? t.getPrelude()
                            : Util.jsFromBindings(t.getTestVariable());


                    // (B) 함수명 / 파라미터 추출
                    // - 코드에서 첫 번째 함수 선언 이름 추출
                    // - 함수 파라미터 이름 리스트 추출
                    String entry = Util.extractFirstFunctionName(reqDTO.getPayload());
                    List<String> params = Util.extractParamNames(reqDTO.getPayload(), entry);

                    // (C) 호출 인자 문자열 생성 (ex: "a, b")
                    String callArgs = String.join(", ", params);


                    // (D) 러너 스니펫 구성
                    // - 함수 호출 후 결과를 console.log로 찍어 비교
                    // - 객체/배열은 JSON.stringify로 직렬화
                    // - 에러 발생 시에도 "" 출력하도록 처리
                    String runner =
                            "\n;(function(){\n" +
                                    "  try {\n" +
                                    "    const __ret = " + entry + "(" + callArgs + ");\n" +
                                    "    if (typeof __ret === 'object') {\n" +
                                    "      console.log(JSON.stringify(__ret));\n" +
                                    "    } else {\n" +
                                    "      console.log(String(__ret));\n" +
                                    "    }\n" +
                                    "  } catch (e) {\n" +
                                    "    // 함수 호출 실패 시에도 비교 로직이 동작하도록 빈 문자열\n" +
                                    "    console.log('');\n" +
                                    "  }\n" +
                                    "})();\n";


                    // (E) 최종 실행 스크립트 조립
                    //   prelude + 사용자 코드 + runner
                    String script = prelude + "\n" + reqDTO.getPayload() + runner;

                    // (F) JS 실행 결과 캡처
                    String result = Util.trimOrNull(Util.executeJs(script));
                    String expected = Util.trimOrNull(t.getTestAnswer());

                    // (G) 기대값 vs 결과 비교
                    if (!Objects.equals(expected, result)) {
                        failures.add(new CheckResponse.FailDTO(
                                false, i, t.getTestVariable(),
                                "오답입니다. 로직을 다시 확인하세요.", expected, result
                        ));
                    }
                    i++;
                }

                // (H) 실패 케이스 있으면 -> 실패 목록 반환
                if (!failures.isEmpty()) {
                    return failures; // List<FailDTO>
                }

                // (I) 모든 케이스 통과 -> 성공 DTO 반환
                return new CheckResponse.DTO(
                        true,  // passed
                        null,  // message
                        null,  // result
                        reqDTO.getPayload()  // 제출 코드 그대로 보존
                );
            });

            // 5. 전체 실행 시간 제한 (2초)
            return Resp.ok(future.get(2, TimeUnit.SECONDS));

        } catch (TimeoutException e) {
            // 6. 시간 초과 (무한루프/비효율 코드)
            return Resp.ok(new CheckResponse.DTO(
                    false, "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)", null, null
            ));
        } catch (Exception e) {
            // 7. 기타 예외 처리 (에러 메시지 가공)
            String msg = ErrorMessageUtil.errorMessage(Optional.ofNullable(e.getMessage()).orElse(""));
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null, null, msg, null, null
            ));
        } finally {
            // 8. 실행기 종료 (자원 정리)
            es.shutdownNow();
        }
    }

}
