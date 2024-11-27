package com.messages;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Messages {
    private static final int numThreads = 10;

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("Exercise4System");

        // Create the server actor
        ActorRef server = system.actorOf(ServerActor.props(), "server");

        // Create the client actor
        ActorRef client = system.actorOf(ClientActor.props(server), "client");

        final ExecutorService exec = Executors.newFixedThreadPool(numThreads);

        exec.submit(() -> {
            // Simulate interaction
            client.tell("Hello", ActorRef.noSender());
            client.tell("How are you?", ActorRef.noSender());
            client.tell(new ServerActor.Sleep(), ActorRef.noSender());
            client.tell("Message during sleep", ActorRef.noSender());
            client.tell("Another message during sleep", ActorRef.noSender());
            client.tell(new ServerActor.Wakeup(), ActorRef.noSender());
            client.tell("Message after wakeup", ActorRef.noSender());
        });

        // Shutdown the system after use
        system.terminate();
    }
}
