package org.example.jjava_compile.dto;

import lombok.Data;

public class CompileRequest {

    @Data
    public static class DTO {
        private String payload;

        public DTO(String payload) {
            this.payload = payload;
        }
    }
}
