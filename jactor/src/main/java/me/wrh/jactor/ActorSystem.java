package me.wrh.jactor;


import me.wrh.jactor.exception.InvalidOperationException;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorSystem {

    /**
     * 创建新的ActorSystem
     *
     * @param name 名字
     * @return
     */
    public static ActorSystem create(String name) {
        return new ActorSystem(name);
    }

    /**
     * Context of actor: /usr
     */
    private final ActorContext context;

    /**
     * Dead letters actor: /sys/deadletters
     */
    private final ActorRef deadLetters;

    /**
     * ActorSystem的名字
     */
    private final String name;

    private final Scheduler scheduler;

    private final long startTime;

    /**
     * Context of actor: /sys
     */
    private final ActorContext sysContext;

    public ActorContext getContext() {
        return context;
    }

    public ActorRef getDeadLetters() {
        return deadLetters;
    }

    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * Actor System运行时间
     *
     * @return
     */
    public long upTime() {
        return System.currentTimeMillis() - startTime;
    }

    public ActorContext getSysContext() {
        return sysContext;
    }

    /**
     * 创建actor
     *
     * @param type 类型
     * @param props 属性
     * @return
     */
    public ActorRef actorOf(Class<? extends Actor> type, Props props) {
        return context.actorOf(type, props);
    }

    /**
     * 创建actor
     *
     * @param type 类型
     * @param props 属性
     * @param name 名字
     * @return
     */
    public ActorRef actorOf(Class<? extends Actor> type, Props props, String name) {
        return context.actorOf(type, props, name);
    }

    public ActorSelection selection(String path) {
        return context.selectionFrom(true, path);
    }

    /**
     * 关闭ActorSystem
     */
    public void shutdown() {
        context.getActor().stopChildren();
        context.stop();
        sysContext.getActor().stopChildren();
        sysContext.stop();
    }

    /**
     * 停止actor
     *
     * @param actor
     */
    public void stop(ActorRef actor) {
        context.stop(actor);
    }

    private ActorSystem(String name) {
        //root: /
        Actor root = new _RootGuardian();
        ActorContext rootContext = new ActorContext(this,
                _RootGuardian.class,
                root,
                ActorPath.RootName,
                Props.None,
                ActorRef.None,
                new ActorPath(ActorPath.SystemName));

        //user: /usr
        Actor userGuardian = new _UserGuardian();
        ActorContext usrGuardianContext = new ActorContext(this,
                _UserGuardian.class,
                userGuardian,
                ActorPath.UsrName,
                Props.None,
                rootContext.getSelf(),
                rootContext.getPath());

        //sys: /sys
        Actor sysGuardian = new _SysGuardian();
        ActorContext sysGuardianContext = new ActorContext(this,
                _SysGuardian.class,
                sysGuardian,
                ActorPath.SysName,
                Props.None,
                rootContext.getSelf(),
                rootContext.getPath());

        this.context = usrGuardianContext;
        this.sysContext = sysGuardianContext;
        this.deadLetters = this.sysContext.actorOf(_DeadLetters.class, Props.None, "deadLetters");
        this.name = name;
        this.scheduler = new Scheduler();
        this.startTime = System.currentTimeMillis();
    }

    static class _DeadLetters extends Actor {
        public void onReceive(Object message) {
            System.out.println("Dead Letter: " + message);
        }
    }

    static class _UserGuardian extends Actor {
        @Override
        public void onReceive(Object message) {
            System.out.println("Usr Guardian: " + message);
        }
    }

    static class _RootGuardian extends Actor {
        @Override
        public void onReceive(Object message) {
            System.out.println("Root Guardian: " + message);
        }
    }

    static class _SysGuardian extends Actor {
        @Override
        public void onReceive(Object message) {
            System.out.println("Sys Guardian: " + message);
        }
    }

    public interface Cancellable {
        void cancel();
        boolean cancelled();
    }

    public static class Scheduler {

        public Cancellable schedule(long delay, long frequency, ActorRef receiver, Object message) {
            if (delay < 0) {
                throw new InvalidOperationException("The delay must be zero or greater");
            }
            if (frequency < 1) {
                throw new InvalidOperationException("The delay must be greater than zero");
            }
            if (receiver == ActorRef.None || message == null) {
                throw new InvalidOperationException("Must provide a receiver and message");
            }
            return null;
        }

    }

    static class ScheduledTimerEvent implements Cancellable {

        private final long frequency;
        private final ActorRef receiver;
        private final Object message;

        private Timer timer;

        private final TimerTask task;

        public ScheduledTimerEvent(long delay, long frequency, ActorRef receiver, Object message) {
            this.frequency = frequency;
            this.receiver = receiver;
            this.message = message;
            this.timer = new Timer();
            this.task = new TimerTask() {
                @Override
                public void run() {
                    if (frequency == 0) {
                        cancel();
                    }
                    receiver.tell(message);
                }
            };
            this.timer.schedule(task, delay, frequency);
        }

        @Override
        public void cancel() {
            timer.cancel();
            timer = null;
        }

        @Override
        public boolean cancelled() {
            return timer == null;
        }
    }

}
