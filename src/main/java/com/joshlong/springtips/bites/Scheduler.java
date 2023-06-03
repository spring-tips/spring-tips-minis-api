package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class Scheduler implements Runnable {

	private final ApplicationEventPublisher publisher;

	private final Repository repository;

	@Bean
	ApplicationListener<ApplicationReadyEvent> launcher(TaskScheduler scheduler) {
		return event -> scheduler.schedule(this, new PeriodicTrigger(Duration.of(1, TimeUnit.MINUTES.toChronoUnit())));
	}

	@Bean
	TaskScheduler taskScheduler() {
		return new TaskSchedulerBuilder().poolSize(10).build();
	}

	@Override
	public void run() {
		this.repository.findNonPromoted().subscribe(springTipToPromote -> this.publisher
				.publishEvent(new SpringTipsBiteScheduleTriggeredEvent(springTipToPromote)));
	}

}
