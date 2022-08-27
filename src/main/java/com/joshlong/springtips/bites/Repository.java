package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

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

	private final boolean resetOnRebuild;

	private final File cloneRepository;

	private final TipManifestReader reader;

	private final Object monitor = new Object();

	private final URI uri;

	private final Set<SpringTip> tips = new ConcurrentSkipListSet<>(
			Comparator.comparing(o -> (o.title() + o.tweetJson())));

	private final ApplicationEventPublisher publisher;

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof RepositoryRefreshEvent || event instanceof ApplicationReadyEvent) {
			this.rebuild();
		}
	}

	@SneakyThrows
	private void rebuild() {

		log.info("reset on rebuild? " + this.resetOnRebuild);

		if (!this.resetOnRebuild)
			return;

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

		var list = new HashSet<SpringTip>();

		var springTips = Objects.requireNonNull(
				this.cloneRepository.listFiles(pathname -> pathname.getAbsolutePath().endsWith(".xml")));

		for (var tip : springTips) {
			log.info("found Spring Tip manifest file [" + tip.getAbsolutePath() + "].");
			try (var f = new InputStreamReader(new FileInputStream(tip))) {
				var xml = this.reader.read(tip, FileCopyUtils.copyToString(f));
				list.add(xml);
			}
		}

		log.debug("clearing old collection of tips");
		synchronized (this.monitor) {
			this.tips.clear();
			this.tips.addAll(list);
		}

		this.publisher.publishEvent(new RepositoryRefreshedEvent(Instant.now(), this.tips));
		log.debug("a new collection of tips is available");

	}

}
