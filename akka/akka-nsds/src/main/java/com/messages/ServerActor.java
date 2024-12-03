package com.messages;

import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

public class ServerActor extends AbstractActorWithStash {
    private boolean isSleeping = false;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Sleep.class, this::handleSleep)
                .match(Wakeup.class, this::handleWakeup)
                .match(Msg.class, this::handleMsg)
                .build();
    }

    private void handleMsg(Msg message) {
        String content = message.getMessage();
        if (isSleeping) {
            System.out.println("SERVER: Storing message (sleeping) - " + content);
            stash();
        } else {
            System.out.println("SERVER: Received and sending message - " + content);
            getSender().tell(message, self());
        }
    }

    private void handleSleep(Sleep msg) {
        System.out.println("SERVER: Going to sleep mode");
        isSleeping = true;
    }

    private void handleWakeup(Wakeup msg) {
        System.out.println("SERVER: Waking up and sending accumulated messages");

        // Resume normal operations
        isSleeping = false;
        unstashAll();
    }

    public static Props props() {
        return Props.create(ServerActor.class);
    }

    // Messages for Sleep and Wakeup
    public static class Sleep {
    }

    public static class Wakeup {
    }
}
