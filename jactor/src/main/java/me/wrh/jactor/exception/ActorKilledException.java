package me.wrh.jactor.exception;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorKilledException extends ActorException {
    public ActorKilledException() {
        super();
    }

    public ActorKilledException(String message) {
        super(message);
    }

    public ActorKilledException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActorKilledException(Throwable cause) {
        super(cause);
    }

    protected ActorKilledException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
