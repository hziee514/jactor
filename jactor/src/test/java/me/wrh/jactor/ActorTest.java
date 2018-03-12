package me.wrh.jactor;

import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorTest {

    @Test
    public void generatedName() {
        ActorSystem system = ActorSystem.create("ActorTest");
        ActorRef actor = system.actorOf(TestActor.class, Props.None);
        String prefix = "/usr/$";
        String path = actor.getPath().getValue();
        system.shutdown();
        assertTrue(path.startsWith(prefix));
        System.out.println(path);
    }

    @Test
    public void givenName() {
        ActorSystem system = ActorSystem.create("ActorTest");
        String name = UUID.randomUUID().toString();
        ActorRef actor = system.actorOf(TestActor.class, Props.None, name);
        String path = actor.getPath().getValue();
        system.shutdown();
        assertEquals("/usr/" + name, path);
        System.out.println(path);
    }

    @Test
    public void forwardOriginal() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorTest");

        CountDownLatch latch = new CountDownLatch(2);

        String originalName = "Original";
        ActorRef forwardTo = system.actorOf(TestForwardToActor.class, Props.with(originalName, latch), "ForwardTo");
        ActorRef forwardFrom = system.actorOf(TestForwardFromActor.class, Props.with(forwardTo), "ForwardFrom");
        ActorRef original = system.actorOf(TestForwardOriginalActor.class, Props.with(forwardFrom), originalName);

        original.tell("testing...");

        Thread.sleep(200);

        system.shutdown();

        assertEquals(1, latch.getCount());
    }

    @Test
    public void numberOfAdditionsAndMultiplications() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorTest");

        CountDownLatch latch = new CountDownLatch(2);

        ActorRef addition = system.actorOf(Addition.class, Props.with(latch));
        ActorRef multiplication = system.actorOf(Multiplication.class, Props.with(latch));

        addition.tell(new Operation(0, 2000000000, 0, multiplication));

        Thread.sleep(100);

        system.stop(addition);
        system.stop(multiplication);
        system.shutdown();

        assertEquals(1, latch.getCount());
    }

    @Test
    public void numberOfAdditionsAndAdditions() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorTest");

        CountDownLatch latch = new CountDownLatch(2);

        ActorRef addition1 = system.actorOf(Addition.class, Props.with(latch));
        ActorRef addition2 = system.actorOf(Addition.class, Props.with(latch));

        addition1.tell(new Operation(0, 1000000, 0, addition2));

        Thread.sleep(4000);

        system.stop(addition1);
        system.stop(addition2);
        system.shutdown();

        assertEquals(1, latch.getCount());
    }

    private static class TestActor extends Actor {

    }

    private static class TestForwardOriginalActor extends Actor {
        private final ActorRef to;
        TestForwardOriginalActor(ActorRef to) {
            this.to = to;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("ORIGINAL: " + message);
            to.tell(getSelf().getPath().getName(), getSelf());
        }
    }

    private static class TestForwardFromActor extends Actor {
        private final ActorRef to;
        TestForwardFromActor(ActorRef to) {
            this.to = to;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("FROM: " + message);
            to.forward(message, getContext());
        }
    }

    private static class TestForwardToActor extends Actor {
        private final CountDownLatch latch;
        private final String expectedPath;
        TestForwardToActor(String expectedPath, CountDownLatch latch) {
            this.expectedPath = expectedPath;
            this.latch = latch;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("To: " + message);
            assertEquals(expectedPath, message);
            latch.countDown();
        }
    }

    private static class Operation {
        final int total;
        final int goal;
        final int operations;
        final ActorRef nextOp;
        Operation(int total, int goal, int operations, ActorRef nextOp) {
            this.total = total;
            this.goal = goal;
            this.operations = operations;
            this.nextOp = nextOp;
        }
    }

    private static class Addition extends Actor {
        private final CountDownLatch latch;
        Addition(CountDownLatch latch) {
            this.latch = latch;
        }
        public void onReceive(Operation message) {
            int total = message.total + 1;
            if (total >= message.goal) {
                latch.countDown();
            } else {
                message.nextOp.tell(new Operation(total, message.goal, message.operations + 1, getSelf()));
            }
        }
    }

    private static class Multiplication extends Actor {
        private final CountDownLatch latch;
        Multiplication(CountDownLatch latch) {
            this.latch = latch;
        }
        public void onReceive(Operation message) {
            int total = message.total * 2;
            if (total >= message.goal) {
                latch.countDown();
            } else {
                message.nextOp.tell(new Operation(total, message.goal, message.operations + 1, getSelf()));
            }
        }
    }

}