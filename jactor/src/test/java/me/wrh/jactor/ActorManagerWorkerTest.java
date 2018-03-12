package me.wrh.jactor;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorManagerWorkerTest {

    @Test
    public void mangerUseWorker() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorManagerWorkerTest");

        CountDownLatch latch = new CountDownLatch(2);

        ActorRef worker = system.actorOf(Worker.class, Props.None,"worker");

        ActorRef manager = system.actorOf(Manager.class, Props.with(worker, latch),"manager");

        manager.tell("start");

        Thread.sleep(100);

        system.stop(worker);

        system.stop(manager);
        Thread.sleep(100);
        system.shutdown();

        assertEquals(latch.getCount(), 1);
    }

    static class Manager extends Actor {

        private ActorRef worker;
        private CountDownLatch latch;

        public Manager(ActorRef worker, CountDownLatch latch) {
            this.worker = worker;
            this.latch = latch;
        }

        @Override
        public void postStop() {
            System.out.println("Manager.postStop");
        }

        @Override
        public void onReceive(Object message) {
            System.out.println("Manager.onReceive: " + message);

            if ("start".equals(message)) {
                worker.tell("hello", getSelf());
                return;
            }
            if ("hello, back".equals(message)) {
                return;
            }
            System.out.println("Manager.onReceive: invalid message: " + message);
        }

        public void onReceive(SpecificMessage message) {
            System.out.println("Manager.onReceive.SpecificMessage: " + message.payload);
            latch.countDown();
        }
    }

    static class Worker extends Actor {
        @Override
        public void onReceive(Object message) {
            System.out.println("Worker.onReceive: " + message);
            if (canReply()) {
                getSender().tell("hello, back", getSelf());
                getSender().tell(SpecificMessage.create("Very Specific"), getSelf());
            } else {
                System.out.println("Worker.onReceive: no reply-to available");
            }
        }

        @Override
        public void postStop() {
            System.out.println("Worker.postStop");
        }
    }

    static class SpecificMessage {
        final String payload;
        private SpecificMessage(String payload) {
            this.payload = payload;
        }
        static SpecificMessage create(String payload) {
            return new SpecificMessage(payload);
        }
    }

}
