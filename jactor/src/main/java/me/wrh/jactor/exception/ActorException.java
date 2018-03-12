package me.wrh.jactor.exception;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorException extends RuntimeException {
    public ActorException() {
        super();
    }

    public ActorException(String message) {
        super(message);
    }

    public ActorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorException(Throwable cause) {
        super(cause);
    }

    protected ActorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
