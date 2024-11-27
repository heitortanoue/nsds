package com.messages;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ClientActor extends AbstractActor {

    private final ActorRef server;

    public ClientActor(ActorRef server) {
        this.server = server;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::handleSendToServer)
                .match(ServerActor.Sleep.class, this::sendSleepToServer)
                .match(ServerActor.Wakeup.class, this::sendWakeupToServer)
                .match(Msg.class, this::handleServerReply)
                .build();
    }

    // Handle text messages differently based on content
    private void handleSendToServer(String message) {
        System.out.println("CLIENT: Sending message to server - " + message);
        server.tell(new Msg(message), self());
    }

    private void sendSleepToServer(ServerActor.Sleep sleep) {
        System.out.println("CLIENT: Sending sleep command to server");
        server.tell(sleep, self());
    }

    private void sendWakeupToServer(ServerActor.Wakeup wakeup) {
        System.out.println("CLIENT: Sending wakeup command to server");
        server.tell(wakeup, self());
    }

    private void handleServerReply(Msg msg) {
        System.out.println("CLIENT: Received reply from server - " + msg.getMessage());
    }

    public static Props props(ActorRef server) {
        return Props.create(ClientActor.class, () -> new ClientActor(server));
    }
}
