package dev.implario.nettier;

import lombok.Getter;

@Getter
public class RemoteException extends Exception {

    private final ErrorLevel errorLevel;

    public RemoteException(ErrorLevel errorLevel, String s) {
        super(s);
        this.errorLevel = errorLevel;
    }

    public RemoteException(ErrorLevel errorLevel, String s, Throwable throwable) {
        super(s, throwable);
        this.errorLevel = errorLevel;
    }


    public enum ErrorLevel {
        FATAL, SEVERE, WARNING, TIMEOUT
    }

}
