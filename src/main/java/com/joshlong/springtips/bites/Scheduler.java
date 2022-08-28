package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.transaction.reactive.TransactionalOperator;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
class Scheduler implements Runnable {

	private final ApplicationEventPublisher publisher;

	private final DatabaseClient dbc;

	private final TransactionalOperator tx;

	private final Function<Map<String, Object>, SpringTip> function = record -> new SpringTip(
			(String) record.get("code"), (String) record.get("title"), (String) record.get("tweet"),
			(String) record.get("uid"));

	@Bean
	ApplicationListener<ApplicationReadyEvent> launcher(TaskScheduler scheduler) {
		return e -> scheduler.schedule(this, new PeriodicTrigger(10, TimeUnit.SECONDS));
	}

	@Bean
	TaskScheduler taskScheduler() {
		return new TaskSchedulerBuilder().poolSize(10).build();
	}

	@Override
	public void run() {
		var sql = """
				select   * from stb_spring_tip_bites where scheduled =
				        (select min(s.scheduled) from stb_spring_tip_bites s where s.promoted is null )

				      """;
		tx.transactional(this.dbc.sql(sql).fetch().all().map(this.function))
				.subscribe(r -> this.publisher.publishEvent(new SpringTipsBiteScheduleTriggeredEvent(r)));
	}

}

class SpringTipsBiteScheduleTriggeredEvent extends ApplicationEvent {

	@Override
	public SpringTip getSource() {
		return (SpringTip) super.getSource();
	}

	SpringTipsBiteScheduleTriggeredEvent(SpringTip source) {
		super(source);
	}

}