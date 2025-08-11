package org.example.jjava_compile._util;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {

    private static final ObjectMapper om = new ObjectMapper();

    // 함수 정의에서 함수명을 찾기 위한 정규식
    private static final Pattern FUNC_NAME =
            Pattern.compile("function\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\(", Pattern.MULTILINE);


    /**
     * payload(사용자 작성 코드)에서 첫 번째로 등장하는 함수 이름을 추출
     */
    public static String extractFirstFunctionName(String payload) {
        Matcher m = FUNC_NAME.matcher(payload);

        if (m.find()) return m.group(1); // 정규식 첫 번째 그룹이 함수명
        return "main"; // 함수 정의가 없을 경우 기본 엔트리 이름으로 "main" 반환
    }

    /**
     * 특정 함수의 매개변수 이름 리스트를 추출
     * 예: function add(x, y) { ... }
     * → ["x", "y"]
     */
    public static List<String> extractParamNames(String payload, String funcName) {
        // funcName에 맞춰 동적으로 정규식 생성
        // ([^)]*) → 닫는 괄호 전까지 모든 문자(매개변수 목록)
        Pattern p = Pattern.compile("function\\s+" + Pattern.quote(funcName) + "\\s*\\(([^)]*)\\)",
                Pattern.MULTILINE);

        Matcher m = p.matcher(payload);
        if (!m.find()) return List.of(); // 해당 함수 정의 없음 → 빈 리스트 반환

        String inside = m.group(1).trim();  // 매개변수 전체 문자열
        if (inside.isEmpty()) return List.of();  // 매개변수 없음 → 빈 리스트 반환

        // 쉼표(,)로 분리 → 양쪽 공백 제거 → 빈 값 필터링
        return Arrays.stream(inside.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 공통: JS 코드 실행 후 출력값 반환
     */
    public static String executeJs(String userCode) {
        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
            // 출력 로그 수집 변수 선언
            context.eval("js", """
                        var outputLogs = [];
                        var console = { log: function(msg) { outputLogs.push(String(msg)); } };
                        var window = { alert: function(msg) { outputLogs.push(String(msg)); } };
                    """);

            // 사용자 코드 실행
            context.eval("js", userCode);

            // 로그 수집
            Value logs = context.getBindings("js").getMember("outputLogs");
            StringBuilder sb = new StringBuilder();
            if (logs != null && logs.hasArrayElements()) {
                for (int i = 0; i < logs.getArraySize(); i++) {
                    sb.append(logs.getArrayElement(i).asString()).append("\n");
                }
            }
            return sb.toString().trim();
        }
    }


    /**
     * 바인딩(Map)을 JS let 선언문으로 변환
     * ex) { n: 987, name: "hi" } -> "let n = 987;\nlet name = \"hi\";\n"
     * 값 직렬화는 JSON을 사용하므로 객체/배열도 안전하게 들어감.
     */
    public static String jsFromBindings(Map<String, Object> bindings) {
        if (bindings == null || bindings.isEmpty()) return "";
        return bindings.entrySet().stream()
                .map(e -> "let " + e.getKey() + " = " + toJsLiteral(e.getValue()) + ";")
                .collect(Collectors.joining("\n")) + "\n";
    }

    /**
     * JS 리터럴 문자열 생성 (ObjectMapper로 JSON 직렬화)
     * 숫자/불린/문자열/배열/객체 전부 대응
     */
    private static String toJsLiteral(Object v) {
        try {
            return om.writeValueAsString(v);
        } catch (Exception e) {
            // 혹시 직렬화 실패 시 안전하게 문자열 처리
            String s = String.valueOf(v).replace("\\", "\\\\").replace("\"", "\\\"");
            return "\"" + s + "\"";
        }
    }

    /**
     * 공통: null 안전 trim
     */
    public static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }


}
