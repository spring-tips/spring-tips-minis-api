package com.joshlong.springtips.bites;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/*
 * integrate the twitter gateway client here
 */
@Slf4j
@Component
class Promoter {

	@EventListener
	public void scheduled(SpringTipsBiteScheduleTriggeredEvent event) {
		var st = event.getSource();
		log.info("promoted: [" + st + "]");
	}

}
