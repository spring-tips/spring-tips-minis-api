package com.joshlong.springtips.bites;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(SpringTipsProperties.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	TipManifestReader tipManifestReader() {
		return new TipManifestReader();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void listEnvironmentVariables() {
		var env = System.getenv();
		env.forEach((k, v) -> log.debug(k + "=" + v));
	}

	@Bean
	Repository repository(TipManifestReader reader, SpringTipsProperties properties,
			ApplicationEventPublisher publisher, DatabaseClient dbc, TransactionalOperator tx) {
		return new Repository(properties.github().cloneDirectory(), reader, properties.github().gitRepository(),
				publisher, dbc, tx);
	}

	@Bean
	Renderer renderer() {
		return new Renderer();
	}

	@Bean
	RepositoryRefreshController repositoryRefreshController(ApplicationEventPublisher publisher,
			SpringTipsProperties properties) {
		return new RepositoryRefreshController(properties.github().rebuildKey(), publisher);
	}

}
