package com.contactlist;

import akka.actor.AbstractActor;
import akka.actor.Props;

import java.util.HashMap;
import java.util.Map;

public class ServerActor extends AbstractActor {

    private final Map<String, String> contactList = new HashMap<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PutMsg.class, this::handlePutMsg)
                .match(GetMsg.class, this::handleGetMsg)
                .build();
    }

    private void handlePutMsg(PutMsg msg) {
        contactList.put(msg.getName(), msg.getEmail());
        System.out.println("SERVER: Added contact - Name: " + msg.getName() + ", Email: " + msg.getEmail());
    }

    private void handleGetMsg(GetMsg msg) {
        String email = contactList.getOrDefault(msg.getName(), "Not Found");
        getSender().tell(new ReplyMsg(msg.getName(), email), self());
    }

    public static Props props() {
        return Props.create(ServerActor.class);
    }

    public static class PutMsg {
        private final String name;
        private final String email;

        public PutMsg(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class GetMsg {
        private final String name;

        public GetMsg(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static class ReplyMsg {
        private final String name;
        private final String email;

        public ReplyMsg(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
