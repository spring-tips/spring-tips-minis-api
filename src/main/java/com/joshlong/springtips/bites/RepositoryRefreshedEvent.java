package com.joshlong.springtips.bites;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Collection;

/**
 * created when the local index has been rebuilt
 */
class RepositoryRefreshedEvent extends ApplicationEvent {

	record RepositoryRefreshedEventContext(Instant instant, Collection<SpringTip> springTips) {
	}

	public RepositoryRefreshedEvent(Instant source, Collection<SpringTip> springTips) {
		super(new RepositoryRefreshedEventContext(source, springTips));
	}

	@Override
	public RepositoryRefreshedEventContext getSource() {
		return (RepositoryRefreshedEventContext) super.getSource();
	}

}
