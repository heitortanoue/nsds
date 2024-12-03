package com.lab.evaluation24;

import akka.actor.AbstractActorWithStash;
import akka.actor.ActorRef;
import akka.actor.Props;

public class BallPasserActor extends AbstractActorWithStash {

    private int passCount = 0;
    private int restBallCount = 0;

    public int W;
    public int R;

    private ActorRef clockwiseNeighbor;
    private ActorRef counterClockwiseNeighbor;


    @Override
    public Receive createReceive() {
        return createReceiveOnNotResting();
    }

    public Receive createReceiveOnResting() {
        return receiveBuilder()
                .match(FinalBallMsg.class, this::onBallReceivedWhileResting)
                .match(ConfigMsg.class, this::onConfig)
                .build();
    }

    public Receive createReceiveOnNotResting() {
        return receiveBuilder()
                .match(FinalBallMsg.class, this::onBallReceivedNotResting)
                .match(ConfigMsg.class, this::onConfig)
                .build();
    }

    private void onBallReceivedNotResting(FinalBallMsg ball)  {
        // if im the originator, drop the ball
        if (!ball.isExternalCommand()) {
            if (ball.getOriginator().equals(getSelf().path().name())) {
                System.out.println(getSelf() + " received its own ball back and drops it.");
                return;
            }
            if (ball.isHotBall()) {
                passCount++;
            }
        }

        ball.setExternalCommand(false);

        if (passCount > W) {
            getContext().become(createReceiveOnResting());
            restBallCount = 1;
            passCount = 0;
            ball.setHotBall(false);

            stash();
            System.out.println(getSelf() + " has exceeded W (" + W + ") and starts resting.");
            return;
        }
        passBall(ball);
    }

    private void onBallReceivedWhileResting(FinalBallMsg ball) {
        restBallCount++;
        ball.setHotBall(false);
        stash();
        System.out.println(getSelf() + " is resting and holds the ball from " + ball.getOriginator());

        if (restBallCount >= R) {
            getContext().become(createReceiveOnNotResting());
            restBallCount = 0;
            System.out.println(getSelf() + " has received " + R + " balls while resting and resumes passing.");
            unstashAll();
        }
    }

    private void onConfig(ConfigMsg msg) {
        this.counterClockwiseNeighbor = msg.getCounterClockwiseNeighbor();
        this.clockwiseNeighbor = msg.getClockwiseNeighbor();
        this.W = msg.getW();
        this.R = msg.getR();
    }

    private void passBall(FinalBallMsg ball) {
        ActorRef nextPlayer = (ball.getDirection() == FinalBallMsg.CLOCKWISE) ? clockwiseNeighbor : counterClockwiseNeighbor;
        System.out.println("passes the ball from " + getSelf() + " to " + nextPlayer.path().name());
        nextPlayer.tell(ball, getSelf());
    }


    public static Props props() {
        return Props.create(BallPasserActor.class, BallPasserActor::new);
    }
}