package me.wrh.jactor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorSelectionTest {

    private ActorSystem system;

    @Test
    public void selectionFromRoot() throws InterruptedException {
        CountDownLatch[] events = selEvents();
        sel1Actor(new ArrayList<>(Arrays.asList(events)));
        Thread.sleep(100);
        system.selection("/usr/sel1").tell("test");
        system.selection("/usr/sel1/sel2").tell("test");
        system.selection("/usr/sel1/sel2/sel3").tell("test");
        system.selection("/usr/sel1/sel2/sel3/sel4").tell("test");
        Thread.sleep(100);
        assertEquals(events[0].getCount(), 0);
        assertEquals(events[1].getCount(), 0);
        assertEquals(events[2].getCount(), 0);
        assertEquals(events[3].getCount(), 0);
        system.shutdown();
        assertTrue(true);
    }

    @Test
    public void selectionFromContext() throws InterruptedException {
        CountDownLatch[] events = selEvents();
        sel1Actor(new ArrayList<>(Arrays.asList(events)));
        Thread.sleep(100);
        system.selection("/usr/sel1").tell("sel2");
        Thread.sleep(100);
        assertEquals(events[0].getCount(), 0);
        assertEquals(events[1].getCount(), 0);
        assertEquals(events[2].getCount(), 0);
        assertEquals(events[3].getCount(), 0);
        system.shutdown();
        assertTrue(true);
    }

    private void sel1Actor(List<CountDownLatch> events) {
        system = ActorSystem.create("ActorSelectionTest");
        system.actorOf(
                Sel1.class,
                Props.with(events),
                "sel1");
    }

    private CountDownLatch[] selEvents() {
        return new CountDownLatch[] {
                new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1), new CountDownLatch(1)
        };
    }

    static class Sel1 extends Actor {
        private List<CountDownLatch> events;
        Sel1(ArrayList<CountDownLatch> events) {
            this.events = events;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("Sel1.onReceive: " + message);
            events.get(0).countDown();
            if (message.equals("sel2")) {
                context.selection("sel2").tell("sel3-4");
            }
        }
        @Override
        public void preStart() {
            System.out.println("Sel1.preStart: ");
            context.actorOf(Sel2.class, Props.with(events), "sel2");
        }
    }

    static class Sel2 extends Actor {
        private List<CountDownLatch> events;
        Sel2(ArrayList<CountDownLatch> events) {
            this.events = events;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("Sel2.onReceive: " + message);
            events.get(1).countDown();
            if (message.equals("sel3-4")) {
                context.selection("sel3").tell("test");
                context.selection("sel3/sel4").tell("test");
            }
        }
        @Override
        public void preStart() {
            System.out.println("Sel2.preStart: ");
            context.actorOf(Sel3.class, Props.with(events), "sel3");
        }
    }

    static class Sel3 extends Actor {
        private List<CountDownLatch> events;
        Sel3(ArrayList<CountDownLatch> events) {
            this.events = events;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("Sel3.onReceive: " + message);
            events.get(2).countDown();
        }
        @Override
        public void preStart() {
            System.out.println("Sel3.preStart: ");
            context.actorOf(Sel4.class, Props.with(events), "sel4");
        }
    }

    static class Sel4 extends Actor {
        private List<CountDownLatch> events;
        Sel4(ArrayList<CountDownLatch> events) {
            this.events = events;
        }
        @Override
        public void onReceive(Object message) {
            System.out.println("Sel4.onReceive: " + message);
            events.get(3).countDown();
        }

        @Override
        public void preStart() {
            System.out.println("Sel3.preStart: ");
        }
    }

}