package com.benson.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.io.Serializable;

@Data
@AllArgsConstructor
public class Result implements Serializable {
    private int code;
    private String message;
    private Object data;
}
