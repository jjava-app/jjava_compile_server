package org.example.jjava_compile._util;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

public class Util {

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
     * 문제 1: 자릿수 합 계산
     * 예: n=987 → 9+8+7=24
     */
    public static boolean checkProblem1(String userCode) {
        // 검산용 테스트 케이스
        int[] testCases = {987, 123, 456, 1001};

        for (int n : testCases) {
            int expected = String.valueOf(Math.abs(n))
                    .chars()
                    .map(Character::getNumericValue)
                    .sum();

            // 사용자 코드에 테스트케이스 주입
            String testCode = "let n = " + n + ";\n" + userCode;
            String output = executeJs(testCode);

            if (!output.equals(String.valueOf(expected))) {
                return false; // 하나라도 틀리면 실패
            }
        }
        return true;
    }
}
