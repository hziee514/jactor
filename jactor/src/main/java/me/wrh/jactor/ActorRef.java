package me.wrh.jactor;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorRef {

    /**
     * no actor
     */
    public static final ActorRef None = null;

    /**
     * no sender
     */
    public static final ActorRef NoSender = null;

    public ActorPath getPath() {
        return this.context.getPath();
    }

    public boolean isTerminated() {
        return this.context.isTerminated();
    }

    public void forward(Object message, ActorContext context) {
        tell(message, context.getSender());
    }

    public void tell(Object message) {
        tell(message, NoSender);
    }

    public void tell(Object message, ActorRef sender) {
        if (!context.isTerminated()) {
            context.enqueue(new ActorContext.Delivery(message, sender));
        } else {
            context.getSystem().getDeadLetters().tell(message, sender);
        }
    }

    ActorContext getContext() {
        return context;
    }

    private final ActorContext context;

    ActorRef(ActorContext context) {
        this.context = context;
    }

}
