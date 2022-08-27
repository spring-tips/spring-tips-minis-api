package com.joshlong.springtips.bites;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;

class RepositoryRefreshEvent extends ApplicationEvent {

	RepositoryRefreshEvent(Instant source) {
		super(source);
	}

	@Override
	public Instant getSource() {
		return (Instant) super.getSource();
	}

}
