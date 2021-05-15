package dev.implario.nettier;

import lombok.Getter;

@Getter
public class NettierException extends RuntimeException {

    public NettierException(String s) {
        super(s);
    }

    public NettierException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
