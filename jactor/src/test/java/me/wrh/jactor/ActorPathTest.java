package me.wrh.jactor;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorPathTest {

    @Test
    public void usrContextPath() {
        ActorSystem system = ActorSystem.create("ActorPathTest");
        assertEquals("/usr", system.getContext().getPath().getValue());
        assertFalse(system.getContext().getPath().isRoot());
        System.out.println(system.getContext().getPath());
        system.shutdown();
    }

    @Test
    public void sysContextPath() {
        ActorSystem system = ActorSystem.create("ActorPathTest");
        assertEquals("/sys", system.getSysContext().getPath().getValue());
        assertFalse(system.getSysContext().getPath().isRoot());
        System.out.println(system.getSysContext().getPath());
        System.out.println(system.getDeadLetters().getPath());
        system.shutdown();
    }

    @Test
    public void sysParentPath() {
        ActorSystem system = ActorSystem.create("ActorPathTest");
        assertEquals(ActorPath.RootName, system.getSysContext().getParent().getPath().getValue());
        assertTrue(system.getSysContext().getParent().getPath().isRoot());
        System.out.println(system.getSysContext().getParent().getPath());
        system.shutdown();
    }

    @Test
    public void usrParentPath() {
        ActorSystem system = ActorSystem.create("ActorPathTest");
        assertEquals(ActorPath.RootName, system.getContext().getParent().getPath().getValue());
        assertTrue(system.getContext().getParent().getPath().isRoot());
        System.out.println(system.getContext().getParent().getPath());
        system.shutdown();
    }

    @Test
    public void actorName() {
        ActorSystem system = ActorSystem.create("ActorPathTest");
        assertEquals("deadLetters", system.getDeadLetters().getPath().getName());
        system.shutdown();
    }

}