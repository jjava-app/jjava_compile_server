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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@RestController
public class CheckController {

    private final Executor compileExecutor; // 비동기 실행 전용 스레드풀 주입 (AsyncConfig에서 정의)


    @PostMapping("/check")
    public ResponseEntity<?> check(@RequestBody CheckRequest.DTO reqDTO) {

        // 1. 지원 언어 검사
        if (!"javascript".equalsIgnoreCase(reqDTO.getType())) {
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null, null, "지원하지 않는 코드 유형입니다.", null, null));
        }

        // 2. 유효성 검사
        if (reqDTO.getPayload() == null || reqDTO.getPayload().isBlank()) {
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null,
                    null, "코드가 비어 있습니다.", null, null
            ));
        }

        try {
            // 채점 로직은 별도 스레드풀(compileExecutor)에서 비동기로 실행
            // - 장점: 컨트롤러 스레드 블로킹 최소화
            // - 아래에서 future.get(timeout)으로 응답 시간 상한을 둔다
            CompletableFuture<Object> future =
                    CompletableFuture.supplyAsync(() -> {

                        // 프론트/메인서버에서 조립해 넘어온 테스트 케이스들
                        final List<CheckRequest.DTO.TestSpecDTO> tests = reqDTO.getTests();

                        // 실패 목록 누적 (한 번에 여러 케이스 실패 가능)
                        List<CheckResponse.FailDTO> failures = new ArrayList<>();

                        Integer i = 0;  // 테스트 케이스 인덱스 (프론트에서 어느 케이스가 틀렸는지 표시용)

                        for (CheckRequest.DTO.TestSpecDTO t : tests) {


                            // (A) 바인딩 주입 코드(prelude) 생성
                            // - testVariable이 { "a":2, "b":3 }이면
                            //   prelude는 "const a = 2; const b = 3;" 형태가 된다.
                            // - 테스트별로 prelude가 직접 지정되어 있으면 그것을 우선 사용
                            String prelude = (t.getPrelude() != null && !t.getPrelude().isBlank())
                                    ? t.getPrelude()
                                    : Util.jsFromBindings(t.getTestVariable());


                            // (B) 함수명/파라미터 추출
                            // - 사용자가 함수만 정의해도 채점기가 직접 호출할 수 있도록
                            //   payload에서 첫 번째 함수 선언을 찾아 이름과 파라미터들을 뽑는다.
                            // - 현재 정규식은 "function foo(x, y) {...}" 형태만 지원
                            //   (화살표 함수/함수 표현식은 필요 시 추가 지원)
                            String entry = Util.extractFirstFunctionName(reqDTO.getPayload());
                            List<String> params = Util.extractParamNames(reqDTO.getPayload(), entry);

                            // (C) 호출 인자 문자열 구성
                            // - params가 ["a","b"]라면 "a, b"
                            // - 바인딩 키와 파라미터명이 동일하다는 전제에서 정상 동작
                            // - 일치하지 않으면 undefined가 전달될 수 있음(출제 시 주의)
                            String callArgs = String.join(", ", params);

                            // (D) 러너 스니펫 구성
                            // - 반환값을 콘솔로 찍어 채점기에서 캡처/비교할 수 있게 한다.
                            // - 반환값이 객체/배열이면 JSON 문자열로 직렬화하여 비교 안정성 확보
                            // - 호출/실행 중 예외가 나더라도 비교 로직이 깨지지 않도록
                            //   빈 문자열을 출력해 실패로 집계되게 한다.
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


                            // (E) 최종 실행 스크립트 조립: [바인딩 주입] + [사용자 코드] + [러너]
                            // - 바인딩 → 사용자 함수정의 → 러너(호출/출력) 순서가 핵심
                            String script = prelude + "\n" + reqDTO.getPayload() + runner;


                            // (F) 실행 및 출력 캡처
                            // - Util.executeJs(script): JS 실행 후 stdout 캡처하여 반환
                            // - Util.trimOrNull: 비교 전에 앞뒤 공백/개행 정리
                            String result = Util.trimOrNull(Util.executeJs(script));
                            String expected = Util.trimOrNull(t.getTestAnswer());


                            // (G) 비교 및 실패 누적
                            // - 현재 비교는 "문자열 기준" 일치 여부
                            // - 숫자/배열/객체는 러너에서 문자열화하여 일관성 확보
                            if (!Objects.equals(expected, result)) {
                                failures.add(new CheckResponse.FailDTO(
                                        false,                       // passed
                                        i,                           // index
                                        t.getTestVariable(),         // failedInputs
                                        "오답입니다. 로직을 다시 확인하세요.", // message
                                        expected,                    // expected
                                        result                       // result
                                ));
                            }
                            i++;
                        }

                        // (H) 실패가 하나라도 있으면 전체 실패 리스트를 반환
                        if (!failures.isEmpty()) {
                            return failures; // List<FailDTO>
                        }

                        // (I) 모든 케이스 통과
                        // - 성공 케이스에서는 간단한 DTO로 ok를 표시
                        // - 필요하면 여기서 리팩토링 요청 트리거, 추가 메타데이터 세팅 등을 수행 가능
                        return new CheckResponse.DTO(
                                true,  // passed
                                null,  // message
                                null,  // result (성공 시 개별 결과는 생략)
                                reqDTO.getPayload()  // 원 코드 보존(추가 처리 용도)
                        );
                    }, compileExecutor);

            // 실행 타임아웃: 무한루프/비효율 코드 방지
            return Resp.ok(future.get(2, TimeUnit.SECONDS));

        } catch (TimeoutException e) {

            // 시간 초과는 별도 메시지로 통일
            return Resp.ok(new CheckResponse.DTO(
                    false, "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)",
                    null, null));
        } catch (Exception e) {
            // 런타임 에러 메시지를 사용자 친화적으로 변환
            String msg = ErrorMessageUtil.errorMessage(Optional.ofNullable(e.getMessage()).orElse(""));
            return Resp.ok(new CheckResponse.FailDTO(
                    false, null,
                    null, msg, null,
                    null
            ));
        }
    }

}
