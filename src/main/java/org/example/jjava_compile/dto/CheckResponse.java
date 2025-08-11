package org.example.jjava_compile.dto;

import lombok.Data;

import java.util.Map;

public class CheckResponse {

    @Data
    public static class DTO {
        private boolean passed; // 성공, 실패 여부
        private String message;      // "정답입니다." / "오답입니다."
        private String result;          // 실제 출력
        private String code;  // 사용자 코드

        public DTO(boolean passed, String message, String result, String code) {
            this.passed = passed;
            this.message = message;
            this.result = result;
            this.code = code;
        }
    }

    @Data
    public static class FailDTO {
        private boolean passed; // 성공, 실패 여부
        private Integer index;                  // 테스트 인덱스(0부터)
        private Map<String, Object> failedInputs;  // bindings 원본
        private String message;      // "정답입니다." / "오답입니다."
        private String expected;            // 기대값
        private String result;              // 실제 출력


        public FailDTO(boolean passed, Integer index, Map<String, Object> failedInputs, String message, String expected, String result) {
            this.passed = passed;
            this.index = index;
            this.failedInputs = failedInputs;
            this.message = message;
            this.expected = expected;
            this.result = result;
        }
    }
}
