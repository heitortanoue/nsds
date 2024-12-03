# Evaluation lab - Akka

## Group number: 12

## Group members

- Enzo Conti 11071898
- Heitor Tanoue de Mello 11071263
- Pietro Pizzoccheri 10797420

## Description of message flows

FinalBallMsg is a wrapper for BallMsg that adds the fields _direction_, _originator_, _isHotBall_
(if `true` the ball is counted on W, changes to `false` when msg is to be stashed*) and _isExternalCommand_
(`true` is sent from the main method, turns `false`).

> *It is to be stashed when the actor is already resting or when that `msg` i
> s the one that makes the actor rest.

We created `createReceiveOnResting` and `createReceiveOnNotResting` to handle the resting and not resting states of the actors.
The configuration of the W, R and both neighbours is done through the `configMsg` received by the actors.
KeepAway creates the 4 actors (A,B,C,D).

1. It then sends a FinalBallMsg to A with attribute hotBall and isExternalCommand to `true` (standard
 when introducing a new ball into the game). 

2. Then A receives it and sends it to B, and so on until A receives the message back and drops the ball.
Same thing for B and C, in the meantime D gets to the threshold and starts resting.

3. When D receives a ball from KeepAway or another peer it stashes the message and set hotBall to `false`.
When the number of balls equals R, D unstashes all the messages it held and resumes his behavior counting from 0.



