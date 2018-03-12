package me.wrh.jactor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wurenhai
 * @since 2018/3/11
 */
public class ActorSelection {

    public void tell(Object message) {
        tell(message, ActorRef.NoSender);
    }

    public void tell(Object message, ActorRef sender) {
        selections.forEach(a -> a.tell(message, sender));
    }

    private final List<ActorRef> selections = new ArrayList<>();

    ActorSelection(ActorRef actor) {
        selections.add(actor);
    }

    ActorSelection(List<ActorRef> actors) {
        selections.addAll(actors);
    }

}
