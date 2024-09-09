package com.lunar.cloud.tunnel.core.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class TunnelException extends RuntimeException {
    private  String hint;
    private  String code;
}
