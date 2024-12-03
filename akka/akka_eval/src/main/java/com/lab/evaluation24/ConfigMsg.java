package com.lab.evaluation24;

import akka.actor.ActorRef;

public class ConfigMsg {
    private final ActorRef counterClockwiseNeighbor;
    private final ActorRef clockwiseNeighbor;

    private final int W;
    private final int R;

    public ConfigMsg(ActorRef counterClockwiseNeighbor, ActorRef clockwiseNeighbor, int W, int R) {
        this.counterClockwiseNeighbor = counterClockwiseNeighbor;
        this.clockwiseNeighbor = clockwiseNeighbor;
        this.W = W;
        this.R = R;
    }

    public ActorRef getCounterClockwiseNeighbor() {
        return counterClockwiseNeighbor;
    }

    public ActorRef getClockwiseNeighbor() {
        return clockwiseNeighbor;
    }

    public int getW(){
        return this.W;
    }

    public int getR(){
        return this.R;
    }
}
