package me.wrh.jactor;

import me.wrh.jactor.exception.InvalidOperationException;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.Callback;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.ThreadFiber;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Actor上下文
 *
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorContext {

    /**
     * 子Actor
     */
    private final List<ActorRef> children;

    /**
     * 父Actor
     */
    private final ActorRef parent;

    /**
     * 当前路径
     */
    private final ActorPath path;

    /**
     * 属性
     */
    private final Props props;

    /**
     * 自身Actor
     */
    private final ActorRef self;

    /**
     * 当前处理的消息发送者
     */
    private ActorRef sender;

    /**
     * Actor系统
     */
    private final ActorSystem system;

    /**
     * 是否已终止
     */
    private volatile boolean terminated;

    public List<ActorRef> getChildren() {
        return new ArrayList<>(children);
    }

    public ActorRef getParent() {
        return parent;
    }

    public ActorPath getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public Props getProps() {
        return props;
    }

    public ActorRef getSelf() {
        return self;
    }

    public ActorRef getSender() {
        return sender;
    }

    public void setSender(ActorRef sender) {
        this.sender = sender;
    }

    public ActorSystem getSystem() {
        return system;
    }

    public boolean isTerminated() {
        return terminated;
    }

    /**
     * 创建子actor
     *
     * @param type 类型
     * @param props 属性
     * @return ActorRef
     */
    public ActorRef actorOf(Class<? extends Actor> type, Props props) {
        return actorOf(type, props, generateActorName());
    }

    /**
     * 创建子actor
     *
     * @param type 类型
     * @param props 属性
     * @param name 名字
     * @return ActorRef
     */
    public ActorRef actorOf(Class<? extends Actor> type, Props props, String name) {
        validateName(name);
        Actor actor = ActorCreator.createWith(type, props);
        ActorContext context = new ActorContext(system, type, actor, name, props, this.self, this.path);
        children.add(context.self);
        return context.self;
    }

    /**
     * 查找actor
     *
     * @param path 路径
     * @return 结果集
     */
    public ActorSelection selection(String path) {
        if (path.startsWith("/")) {
            return system.selection(path);
        }
        return selectionFrom(false, path);
    }

    @SuppressWarnings("unused")
    public void become(Receiver receiver) {
        become(receiver, true);
    }

    public void become(Receiver receiver, boolean discardOld) {
        if (discardOld) {
            unbecome();
        }
        this.receiver = receiver;
        this.receivers.push(receiver);
        channel.clearSubscribers();
        channel.subscribe(fiber, getOnReceiveDelegate());
    }

    /**
     * 停止actor
     *
     * @param actor ActorRef
     */
    public void stop(ActorRef actor) {
        if (actor == self) {
            parent.getContext().stop(self);
        } else {
            if (children.remove(actor)) {
                actor.getContext().stop();
            } else {
                sweep(actor, getChildren());
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    public void unbecome() {
        if (receivers.size() > 1) {
            receivers.pop();
        }
        this.receiver = receivers.pop();
        channel.clearSubscribers();
        channel.subscribe(fiber, getOnReceiveDelegate());
    }

    ActorContext(ActorSystem system,
                 Class<? extends Actor> actorType,
                 Actor actor,
                 String name,
                 Props props,
                 ActorRef parent,
                 ActorPath parentPath) {
        this(system, actorType, actor, name, props, parent, parentPath, false, new MemoryChannel<>(), new ThreadFiber());
    }

    /**
     *
     * @param system ActorSystem引用
     * @param actorType Actor类型
     * @param actor Actor实例
     * @param name 名字
     * @param props 属性
     * @param parent 父Actor
     * @param parentPath 路径
     * @param suspended 是否挂起
     * @param channel 消息管道
     * @param fiber 执行纤程
     */
    private ActorContext(ActorSystem system,
                         Class<? extends Actor> actorType,
                         Actor actor,
                         String name,
                         Props props,
                         ActorRef parent,
                         ActorPath parentPath,
                         boolean suspended,
                         MemoryChannel<Delivery> channel,
                         Fiber fiber) {
        this.system = system;
        this.actor = actor;
        this.actor.setContext(this);
        this.channel = channel;
        this.fiber = fiber;
        this.children = new LinkedList<>();
        this.parent = parent;
        this.path = parentPath.withName(name);
        this.props = props;
        this.receivers = new Stack<>();
        this.self = new ActorRef(this);
        this.sender = ActorRef.NoSender;
        this.suspended = suspended;
        this.terminated = false;
        this.type = actorType;

        start();
    }

    /**
     * Actor实例
     */
    private final Actor actor;

    private final MemoryChannel<Delivery> channel;

    private final Fiber fiber;

    private Receiver receiver;

    private volatile boolean suspended;

    private final Class<? extends Actor> type;

    Actor getActor() {
        return actor;
    }

    void enqueue(Delivery delivery) {
        channel.publish(delivery);
    }

    @SuppressWarnings("unused")
    boolean hasChildren() {
        return !children.isEmpty();
    }

    void stop() {
        suspended = true;
        actor.stopChildren();
        terminated = true;
        stopConcurrency();
        actor.postStop();
        stopContext();
    }

    private final Stack<Receiver> receivers;

    private AtomicLong nameIndex = new AtomicLong(10000L);

    private ActorRef actorOf(Class<? extends Actor> actorType,
                             Props props,
                             String name,
                             boolean suspended,
                             MemoryChannel<Delivery> channel,
                             Fiber fiber) {
        validateName(name);
        Actor actor = ActorCreator.createWith(actorType, props);
        ActorContext context = new ActorContext(system, actorType, actor, name, props, self, path, suspended, channel, fiber);
        children.add(context.self);
        return context.self;
    }

    ActorSelection selectionFrom(boolean root, String path) {
        // split root path will make the first element empty
        return selectionFrom(root, path.split("/"), root ? 1 : 0);
    }

    private ActorSelection selectionFrom(boolean root, String[] path, int index) {
        if (root && path[index].equals(ActorPath.UsrName)) {
            ++index;
        }
        List<ActorRef> children = getChildren();
        for (ActorRef child : children) {
            if (child.getPath().getName().equals(path[index])) {
                if ((index + 1) == path.length) {
                    return new ActorSelection(child);
                } else {
                    return child.getContext().selectionFrom(false, path, index + 1);
                }
            }
        }
        return new ActorSelection(system.getDeadLetters());
    }

    private void checkSuspended() {
        while (suspended) {
            if (terminated) {
                break;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String generateActorName() {
        long value = nameIndex.getAndIncrement();
        byte[] bytes = String.valueOf(value).getBytes();
        return "$" + Base64.getEncoder().encodeToString(bytes);
    }

    private Callback<Delivery> getOnReceiveDelegate() {
        return delivery -> {
            checkSuspended();
            this.sender = delivery.sender;
            try {
                try {
                    Method m = receiver.getClass().getDeclaredMethod("onReceive", delivery.message.getClass());
                    m.setAccessible(true);
                    m.invoke(receiver, delivery.message);
                } catch (NoSuchMethodException e) {
                    receiver.onReceive(delivery.message);
                }
            } catch (Exception e) {
                handleFailure(e, delivery.message);
            }
            this.sender = ActorRef.NoSender;
        };
    }

    private void handleFailure(Exception reason, Object message) {
        parent.getContext().recoverFromFor(reason, message, self);
    }

    private void recoverByEscalation(Exception reason) {
        parent.getContext().recoverFromFor(reason, null, self);
    }

    private void recoverByRestarting(Exception reason, Object message, ActorRef child) {
        ActorContext childContext = child.getContext();
        try {
            childContext.suspended = true;
            childContext.actor.preRestart(reason, message);
            children.remove(child);
            ActorRef restarted = actorOf(childContext.type,
                    childContext.props,
                    childContext.path.getName(),
                    true,
                    childContext.channel,
                    childContext.fiber);
            restarted.getContext().suspended = false;
            childContext.stopContext();
            restarted.getContext().getActor().postRestart(reason);
        } finally {
            childContext.suspended = false;
        }
    }

    @SuppressWarnings("unused")
    private void recoverByResume(Exception reason, ActorRef child) {
        //ignore
    }

    @SuppressWarnings("unused")
    private void recoverByStopping(Exception reason, ActorRef child) {
        stop(child);
    }

    private void recoverFromFor(Exception reason, Object message, ActorRef child) {
        Actor.SupervisorStrategy.Directive directive = actor.getStrategy().decide(reason);
        switch (directive) {
            case Restart:
                recoverByRestarting(reason, message, child);
                break;
            case Stop:
                recoverByStopping(reason, child);
                break;
            case Escalate:
                recoverByEscalation(reason);
                break;
            case Resume:
                recoverByResume(reason, child);
                break;
        }
    }

    private void start() {
        become(actor, false);
        if (!suspended) {
            actor.preStart();
            fiber.start();
        }
    }

    private void stopConcurrency() {
        channel.clearSubscribers();
        fiber.dispose();
    }

    private void stopContext() {
        this.actor.setContext(null);
        this.children.clear();
        this.receivers.clear();
        this.sender = ActorRef.NoSender;
        this.suspended = true;
        this.terminated = true;
    }

    private void sweep(ActorRef actor, List<ActorRef> children) {
        for (ActorRef child : children) {
            if (child == actor) {
                child.getContext().getParent().getContext().stop(child);
                return;
            }
        }
        for (ActorRef child : children) {
            sweep(actor, child.getContext().getChildren());
        }
    }

    private void validateName(String name) {
        if (name == null || name.length() == 0) {
            throw new InvalidOperationException("The actor name is required");
        }
        if (name.substring(1).contains("$") || name.contains("/")) {
            throw new InvalidOperationException("The actor name contains invalid character(s)");
        }
        for (ActorRef child : children) {
            if (name.equals(child.getPath().getName())) {
                throw new InvalidOperationException("The actor name is not unique");
            }
        }
    }

    static class Delivery {

        private final Object message;
        private final ActorRef sender;

        Delivery(Object message, ActorRef sender) {
            this.message = message;
            this.sender = sender;
        }

    }

    interface Receiver {
        void onReceive(Object message);
    }

    static class ActorCreator {

        static Actor createWith(Class<?> type, Props props) {
            int count = props.getCount();
            int index = 0;
            Class<?>[] types = new Class<?>[count];
            Object[] args = new Object[count];
            for (Object arg : props.getValues()) {
                types[index] = arg.getClass();
                args[index] = arg;
                ++index;
            }
            try {
                Constructor<?> ctor = type.getDeclaredConstructor(types);
                ctor.setAccessible(true);
                return (Actor)ctor.newInstance(args);
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
                throw new InvalidOperationException("No constructor of type: " + type.getName());
            }
        }

    }

}
