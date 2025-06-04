package dev.donutquine.swf.exceptions;

public class TooManyObjectsException extends LoadingFaultException {
    public TooManyObjectsException(String message) {
        super(message);
    }
}
