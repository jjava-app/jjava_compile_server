package org.example.jjava_compile.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

public class CheckRequest {

    @Data
    public static class DTO {
        private String type;
        private String payload;
        private List<TestSpecDTO> tests;

        @Data
        public static class TestSpecDTO {
            private String prelude;
            private Map<String, Object> testVariable;
            private String testAnswer;

            public TestSpecDTO(String prelude, Map<String, Object> testVariable, String testAnswer) {
                this.prelude = prelude;
                this.testVariable = testVariable;
                this.testAnswer = testAnswer;
            }
        }


        public DTO(String type, String payload, List<TestSpecDTO> tests) {
            this.type = type;
            this.payload = payload;
            this.tests = tests;
        }
    }
}
