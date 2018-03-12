package me.wrh.jactor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorSystemTest {

    @Test
    public void start() {
        ActorSystem system = ActorSystem.create("ActorSystemTest");
        assertEquals("ActorSystemTest", system.getName());
        system.shutdown();
    }

    @Test
    public void startWithUpTime() throws InterruptedException {
        long preSystem = System.currentTimeMillis();
        Thread.sleep(10);
        ActorSystem system = ActorSystem.create("ActorSystemTest");
        Thread.sleep(150);
        assertTrue(preSystem < system.getStartTime());
        assertTrue(system.upTime() > 100);
        system.shutdown();
    }

    @Test
    public void deadLetters() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorSystemTest");
        system.getDeadLetters().tell("testing dead letters");
        Thread.sleep(200);
        system.shutdown();
    }

    @Test
    public void deadSelection() throws InterruptedException {
        ActorSystem system = ActorSystem.create("ActorSystemTest");
        ActorSelection selection = system.selection("/usr/NonExistingActor42");
        system.getDeadLetters().tell("testing dead letters");
        Thread.sleep(200);
        system.shutdown();
    }

}