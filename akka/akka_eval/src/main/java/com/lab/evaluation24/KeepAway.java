package com.lab.evaluation24;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import java.util.concurrent.TimeUnit;

public class KeepAway {

    public final static int W = 2;
    public final static int R = 3;

    public static void main(String[] args) {

        final ActorSystem sys = ActorSystem.create("System");

        // Create actors representing players
        ActorRef a = sys.actorOf(BallPasserActor.props(), "A");
        ActorRef b = sys.actorOf(BallPasserActor.props(), "B");
        ActorRef c = sys.actorOf(BallPasserActor.props(), "C");
        ActorRef d = sys.actorOf(BallPasserActor.props(), "D");

        // Set neighbors for each player
        a.tell(new ConfigMsg(d, b, W, R), ActorRef.noSender());
        b.tell(new ConfigMsg(a, c, W, R), ActorRef.noSender());
        c.tell(new ConfigMsg(b, d, W, R), ActorRef.noSender());
        d.tell(new ConfigMsg(c, a, W, R), ActorRef.noSender());

        // Wait until system is ready
        sleep(1);

        // A sends a ball clockwise, it receives it back and drops it
        System.out.println("\n=== A sends a ball clockwise ===");
        a.tell(new FinalBallMsg("A", BallMsg.CLOCKWISE, true, true), ActorRef.noSender());

        sleep(2);

        // B sends a ball counterclockwise, it receives it back and drops it
        System.out.println("\n=== B sends a ball counterclockwise ===");
        b.tell(new FinalBallMsg("B", BallMsg.COUNTERCLOCKWISE, true, true), ActorRef.noSender());

        sleep(2);

        // C sends a ball counterclockwise, the ball gets to D that is put to rest
        System.out.println("\n=== C sends a ball counterclockwise ===");
        c.tell(new FinalBallMsg("C", BallMsg.COUNTERCLOCKWISE, true, true), ActorRef.noSender());

        sleep(2);

        // D sends a ball clockwise, but it's resting
        System.out.println("\n=== D sends a ball clockwise while resting ===");
        d.tell(new FinalBallMsg("D", BallMsg.CLOCKWISE, true, true), ActorRef.noSender());

        // D sends another ball clockwise, it's now at R balls while resting and resumes
        System.out.println("\n=== D sends another ball clockwise ===");
        d.tell(new FinalBallMsg("D", BallMsg.CLOCKWISE, true, true), ActorRef.noSender());

        sleep(4);

        // Wait until system is ready again
        sleep(2);
        sys.terminate();
    }

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
    }
}