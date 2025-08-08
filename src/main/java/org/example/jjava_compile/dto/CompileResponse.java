package org.example.jjava_compile.dto;

import lombok.Data;

public class CompileResponse {

    @Data
    public static class DTO {
        private String code;
        private String result;

        public DTO(String code, String result) {
            this.code = code;
            this.result = result;
        }
    }
}
