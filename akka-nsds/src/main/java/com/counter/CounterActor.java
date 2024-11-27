package com.counter;

import akka.actor.AbstractActor;
import akka.actor.AbstractActorWithStash;
import akka.actor.Props;

public class CounterActor extends AbstractActorWithStash {

	private int counter;

	public CounterActor() {
		this.counter = 0;
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(SimpleMessage.class, this::onMessage).build();
	}

	void onMessage(SimpleMessage msg) {
		if (msg instanceof IncrementMessage) {
			counter++;
			unstash();
		} else if (msg instanceof DecrementMessage) {
			// now, we add a condition to decrement only if the counter is greater than 0
			// otherwise, we stash the message
			if (counter <= 0) {
				stash();
				return;
			}
			counter--;
		}
		System.out.println("Counter changed to " + counter);
	}

	static Props props() {
		return Props.create(CounterActor.class);
	}

}
