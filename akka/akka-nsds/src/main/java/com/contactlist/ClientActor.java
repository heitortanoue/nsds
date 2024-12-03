package com.contactlist;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.Future;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

public class ClientActor extends AbstractActor {

    private final ActorRef server;

    public ClientActor(ActorRef server) {
        this.server = server;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, this::processCommand)
                .match(ServerActor.ReplyMsg.class, this::handleReply)
                .build();
    }

    private void processCommand(String command) throws InterruptedException, TimeoutException {
        if (command.startsWith("put")) {
            String[] parts = command.split(" ");
            if (parts.length == 3) {
                String name = parts[1];
                String email = parts[2];
                server.tell(new ServerActor.PutMsg(name, email), self());
            } else {
                System.out.println("CLIENT: Invalid put command. Use: put <name> <email>");
            }
        } else if (command.startsWith("get")) {
            String[] parts = command.split(" ");
            if (parts.length == 2) {
                String name = parts[1];
                Future<Object> future = Patterns.ask(server, new ServerActor.GetMsg(name), 5000);

                Await.result(future, scala.concurrent.duration.Duration.Inf());

                ServerActor.ReplyMsg reply = (ServerActor.ReplyMsg) future.value().get().get();

                System.out.println("CLIENT: Received reply - Name: " + reply.getName() + ", Email: " + reply.getEmail());
            } else {
                System.out.println("CLIENT: Invalid get command. Use: get <name>");
            }
        } else {
            System.out.println("CLIENT: Unknown command. Use: put <name> <email> or get <name>");
        }
    }

    private void handleReply(ServerActor.ReplyMsg msg) {
        System.out.println("CLIENT: Received reply - Name: " + msg.getName() + ", Email: " + msg.getEmail());
    }

    public static Props props(ActorRef server) {
        return Props.create(ClientActor.class, () -> new ClientActor(server));
    }
}
