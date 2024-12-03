package com.lab.evaluation24;

public class FinalBallMsg extends BallMsg {
    private final int direction;
    private final String originator;
    private boolean isHotBall; // hot ball is a ball that counts to the actor's threshold
    private boolean isExternalCommand;

    public FinalBallMsg(String originator, int direction, boolean isHotBall, boolean isExternalCommand) {
        this.originator = originator;
        this.direction = direction;
        this.isHotBall = isHotBall;
        this.isExternalCommand = isExternalCommand;
    }

    public String getOriginator() {
        return originator;
    }

    public int getDirection() {
        return direction;
    }

    public boolean isHotBall() {
        return isHotBall;
    }

    public void setHotBall(boolean isHotBall) {
        this.isHotBall = isHotBall;
    }

    public boolean isExternalCommand() {
        return isExternalCommand;
    }

    public void setExternalCommand(boolean isExternalCommand) {
        this.isExternalCommand = isExternalCommand;
    }
}
