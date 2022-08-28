package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
@RequiredArgsConstructor
class Repository implements ApplicationListener<ApplicationEvent> {

	private final File cloneRepository;

	private final TipManifestReader reader;

	private final Object monitor = new Object();

	private final URI uri;

	private final Set<SpringTip> tips = new ConcurrentSkipListSet<>(
			Comparator.comparing(o -> (o.title() + o.tweetJson())));

	private final ApplicationEventPublisher publisher;

	private final DatabaseClient dbc;

	private final TransactionalOperator tx;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof RepositoryRefreshEvent || event instanceof ApplicationReadyEvent) {
			this.rebuild();
		}
		if (event instanceof RepositoryRefreshedEvent rre)
			this.persist(rre);

	}

	@SneakyThrows
	private static Mono<SpringTip> doPersist(DatabaseClient dbc, SpringTip tip) {
		var sql = """



				 insert into  stb_spring_tip_bites(
				    scheduled,
				    tweet,
				    uid,
				    title ,
				    code
				)
				values (
				    (coalesce( (select max(scheduled)::date  + interval '2 days' from stb_spring_tip_bites) , NOW()::date ) ),
				    :tweet,
				    :uid,
				    :title,
				    :code
				)
				on conflict on constraint stb_spring_tip_bites_uid_key
				do update set
				     tweet  = excluded.tweet,
				    code  = excluded.code,
				    title  = excluded.title
				""";

		return dbc.sql(sql) //
				.bind("tweet", tip.tweetJson()) //
				.bind("uid", tip.uid())//
				.bind("title", tip.title())//
				.bind("code", tip.code())//
				.fetch().rowsUpdated().map(x -> tip);

	}

	@SneakyThrows
	private void persist(RepositoryRefreshedEvent rre) {
		var tipCollection = rre.getSource().springTips();
		log.info("going to persist " + tipCollection.size() + " elements");
		var tips = Flux //
				.fromIterable(tipCollection)//
				.flatMap(springTip -> doPersist(this.dbc, springTip));
		this.tx.transactional(tips).doOnError(e -> log.error("something went wrong!", e))
				.subscribe(st -> log.info(st.toString()));
	}

	@SneakyThrows
	private void rebuild() {

		if (this.cloneRepository.exists() && this.cloneRepository.isDirectory()) {
			log.info("deleting " + this.cloneRepository.getAbsolutePath() + '.');
			FileSystemUtils.deleteRecursively(this.cloneRepository);
		}
		log.info("going to clone " + this.uri + " to " + this.cloneRepository.getAbsolutePath());
		var repo = Git.cloneRepository()//
				.setDirectory(this.cloneRepository) //
				.setURI(this.uri.toString()) //
				.call() //
				.getRepository();
		log.info("git clone'd " + repo.toString());

		var collection = new HashSet<SpringTip>();

		var springTips = Objects.requireNonNull(
				this.cloneRepository.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".xml")));

		for (var tip : springTips) {
			log.info("found Spring Tip manifest file [" + tip.getAbsolutePath() + "].");
			try (var f = new InputStreamReader(new FileInputStream(tip))) {
				var xml = this.reader.read(tip, FileCopyUtils.copyToString(f));
				collection.add(xml);
			}
		}
		log.info("the collection has " + collection.size() + " elements");
		synchronized (this.monitor) {
			this.tips.clear();
			this.tips.addAll(collection);
		}
		this.publisher.publishEvent(new RepositoryRefreshedEvent(Instant.now(), this.tips));
	}

}
