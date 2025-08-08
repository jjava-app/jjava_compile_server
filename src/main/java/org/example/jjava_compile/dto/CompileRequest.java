package org.example.jjava_compile.dto;

import lombok.Data;

public class CompileRequest {

    @Data
    public static class DTO {
        private String type;
        private String payload;

        public DTO(String type, String payload) {
            this.type = type;
            this.payload = payload;
        }
    }
}
