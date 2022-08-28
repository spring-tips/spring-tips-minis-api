package com.joshlong.springtips.bites;

import org.springframework.context.ApplicationEvent;

class SpringTipsBiteScheduleTriggeredEvent extends ApplicationEvent {

	@Override
	public SpringTip getSource() {
		return (SpringTip) super.getSource();
	}

	SpringTipsBiteScheduleTriggeredEvent(SpringTip source) {
		super(source);
	}

}
