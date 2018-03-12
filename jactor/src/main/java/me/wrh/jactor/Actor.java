package me.wrh.jactor;

import me.wrh.jactor.exception.ActorInitializationException;
import me.wrh.jactor.exception.ActorKilledException;
import me.wrh.jactor.exception.InvalidOperationException;

import java.util.LinkedList;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public abstract class Actor implements ActorContext.Receiver {

    public void preStart() {}

    public void postStop() {}

    public void preRestart(Exception reason, Object message) {
        stopChildren();
        postStop();
    }

    public void postRestart(Exception reason) {
        preStart();
    }

    void setContext(ActorContext context) {
        this.context = context;
    }

    void stopChildren() {
        context.getChildren().forEach(context::stop);
    }

    protected ActorContext context;

    private SupervisorStrategy strategy;

    Actor() {
        strategy = DefaultStrategy;
    }

    ActorContext getContext() {
        return context;
    }

    ActorRef getSender() {
        return context.getSender();
    }

    void setSender(ActorRef sender) {
        context.setSender(sender);
    }

    protected ActorRef getSelf() {
        return context.getSelf();
    }

    protected boolean canReply() {
        return getSender() != null;
    }

    SupervisorStrategy getStrategy() {
        return strategy;
    }

    private static final SupervisorStrategy DefaultStrategy = new OneForOneStrategy();

    @Override
    public void onReceive(Object message) {
        
    }

    public static class SupervisorStrategy{

        public enum Directive {
            Escalate, Restart, Resume, Stop
        }

        protected enum StrategyType {
            AllForOne, OneForOne
        }

        private final StrategyType type;

        public Directive decide(Exception e) {
            if (e instanceof ActorInitializationException) {
                return Directive.Stop;
            }
            if (e instanceof ActorKilledException) {
                return Directive.Stop;
            }
            if (e instanceof InvalidOperationException) {
                return Directive.Restart;
            }
            if (e instanceof RuntimeException) {
                return Directive.Restart;
            }
            return Directive.Escalate;
        }

        SupervisorStrategy(StrategyType type) {
            this.type = type;
        }
    }

    public static final class AllForOneStrategy extends SupervisorStrategy {
        public AllForOneStrategy() {
            super(StrategyType.AllForOne);
        }
    }

    public static final class OneForOneStrategy extends SupervisorStrategy {
        OneForOneStrategy() {
            super(StrategyType.OneForOne);
        }
    }

}
