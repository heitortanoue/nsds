package com.contactlist;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.Scanner;

public class ContactList {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("ContactListSystem");

        // Create the server actor
        ActorRef server = system.actorOf(ServerActor.props(), "server");

        // Create the client actor
        ActorRef client = system.actorOf(ClientActor.props(server), "client");

        // Simulate interaction with the client
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter commands (put <name> <email>, get <name>, or exit):");

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equalsIgnoreCase(input)) {
                break;
            }
            client.tell(input, ActorRef.noSender());
        }

        system.terminate();
    }
}
