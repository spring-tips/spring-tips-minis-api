package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.InputStreamResource;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.FileSystemUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
class Repository implements ApplicationListener<ApplicationEvent> {

	private final File cloneRepository;

	private final TipManifestReader reader;

	private final URI uri;

	private final ApplicationEventPublisher publisher;

	private final DatabaseClient dbc;

	private final TransactionalOperator tx;

	private final Function<Map<String, Object>, SpringTip> mapSpringTipFunction = //
			record -> new SpringTip((String) record.get("code"), (String) record.get("title"),
					(String) record.get("tweet"), (String) record.get("uid"));

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		// if we got a webhook or the app has started...
		if (event instanceof RepositoryRefreshEvent || event instanceof ApplicationReadyEvent) {
			this.rebuild();
		}
		if (event instanceof RepositoryRefreshedEvent rre)
			this.persist(rre);

	}

	Flux<SpringTip> findNonPromoted() {
		var sql = """
				  select * from stb_spring_tip_bites where
				  scheduled < now() and
				  scheduled = (select min(s.scheduled) from stb_spring_tip_bites s where s.promoted is null )
				""";
		var results = this.dbc.sql(sql).fetch().all().map(this.mapSpringTipFunction);
		return this.tx.transactional(results);
	}

	Mono<SpringTip> findById(Integer id) {
		var results = this.dbc //
				.sql("select * from stb_spring_tip_bites where id = :id ")//
				.bind("id", id)//
				.fetch()//
				.one()//
				.map(this.mapSpringTipFunction);
		return this.tx.transactional(results);
	}

	Flux<SpringTip> findAll() {
		var results = this.dbc //
				.sql("select * from stb_spring_tip_bites ")//
				.fetch()//
				.all()//
				.map(this.mapSpringTipFunction);
		return this.tx.transactional(results);
	}

	@SneakyThrows
	private Mono<SpringTip> doPersist(SpringTip tip) {
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
				  	tweet = excluded.tweet,
				     code = excluded.code,
				    title = excluded.title
				""";

		return dbc.sql(sql) //
				.bind("tweet", tip.tweet()) //
				.bind("uid", tip.uid())//
				.bind("title", tip.title())//
				.bind("code", tip.code())//
				.fetch() //
				.rowsUpdated() //
				.map(x -> tip);

	}

	@SneakyThrows
	private void persist(RepositoryRefreshedEvent rre) {
		var tipCollection = rre.getSource().springTips();
		log.info("going to persist " + tipCollection.size() + " elements");
		var tips = Flux //
				.fromIterable(tipCollection)//
				.flatMap(this::doPersist);
		this.tx.transactional(tips) //
				.doOnError(e -> log.error("something went wrong!", e)) //
				.subscribe(st -> log.info("\t" + st.uid() + " " + st.title()));
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
		var springTips = Objects.requireNonNull(
				this.cloneRepository.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".xml")));
		var ingestedTips = new HashSet<SpringTip>();
		for (var tip : springTips) {
			try (var inputStream = new FileInputStream(tip)) {
				var xml = ResourceUtils.read(new InputStreamResource(inputStream));
				var springTip = this.reader.read(xml);
				ingestedTips.add(springTip);
			}
		}
		var out = ingestedTips.stream()//
				.sorted(Comparator.comparing(SpringTip::uid))//
				.toList();
		this.publisher.publishEvent(new RepositoryRefreshedEvent(Instant.now(), out));
	}

}
