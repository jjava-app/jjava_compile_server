package org.example.jjava_compile._util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@AllArgsConstructor
@Data
public class Resp<T> {
    private Integer status;
    private String msg;
    private T body;

    // ResponseEntity 형태로 바로 리턴 (성공)
    public static <B> ResponseEntity<Resp<B>> ok(B body) {
        Resp<B> resp = new Resp<>(200, "성공", body);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    // ResponseEntity 형태로 바로 리턴 (실패)
    public static ResponseEntity<Resp<?>> fail(HttpStatus status, String msg) {
        Resp<?> resp = new Resp<>(status.value(), msg, null);
        return new ResponseEntity<>(resp, status);
    }

    public static Resp<?> fail(Integer status, String msg) { // 스프링 도움 안받을때!!
        Resp<?> resp = new Resp<>(status, msg, null);
        return resp;
    }
}