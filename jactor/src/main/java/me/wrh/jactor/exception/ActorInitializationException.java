package me.wrh.jactor.exception;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorInitializationException extends ActorException {
    public ActorInitializationException() {
        super();
    }

    public ActorInitializationException(String message) {
        super(message);
    }

    public ActorInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorInitializationException(Throwable cause) {
        super(cause);
    }

    protected ActorInitializationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
