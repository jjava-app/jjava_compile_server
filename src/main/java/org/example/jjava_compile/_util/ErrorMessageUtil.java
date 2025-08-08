package org.example.jjava_compile._util;

public class ErrorMessageUtil {

    public static String errorMessage(String message) {
        if (message == null || message.isBlank()) return "알 수 없는 오류가 발생했습니다.";

        message = message.toLowerCase();

        if (message.contains("timeout")) {
            return "실행 시간이 너무 오래 걸립니다 (무한루프 또는 비효율적인 코드)";
        }

        if (message.contains("syntaxerror")) {
            return "문법 오류가 있습니다. 세미콜론 누락이나 괄호 짝을 확인하세요.";
        }
        if (message.contains("referenceerror")) {
            return "정의되지 않은 변수를 사용하고 있습니다.";
        }
        if (message.contains("typeerror")) {
            return "잘못된 타입의 값을 사용하고 있습니다. 함수 호출이나 속성 접근을 확인하세요.";
        }
        if (message.contains("rangeerror")) {
            return "허용되지 않는 범위의 값이 사용되었습니다.";
        }
        if (message.contains("internalerror")) {
            return "내부 오류가 발생했습니다.";
        }
        if (message.contains("stack overflow") || message.contains("maximum call stack size exceeded")) {
            return "재귀 호출이 너무 많아 스택이 초과되었습니다.";
        }
        if (message.contains("timeout")) {
            return "실행 시간이 너무 길어 중단되었습니다.";
        }

        // fallback
        return "JS 실행 중 오류가 발생했습니다: " + message;
    }
}
