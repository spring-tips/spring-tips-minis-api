package com.joshlong.springtips.bites;

import com.joshlong.twitter.Twitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Date;

/*
 * integrate the twitter gateway client here
 */
@Slf4j
@RequiredArgsConstructor
class Promoter {

	private final Twitter twitter;

	private final DatabaseClient dbc;

	private final TransactionalOperator tx;

	private final String twitterUsername, twitterClientId, twitterClientSecret;

	private final Renderer renderer;

	@EventListener
	public void scheduled(SpringTipsBiteScheduleTriggeredEvent event) {
		var springTip = event.getSource();
		var client = new Twitter.Client(this.twitterClientId, this.twitterClientSecret);
		var media = this.renderer.render(springTip);
		var image = new Twitter.Media(media, Twitter.MediaType.IMAGE);
		var promotionPipeline = this.twitter//
				.scheduleTweet(client, new Date(), this.twitterUsername, springTip.tweet(), image) //
				.flatMap(result -> {
					if (result) {
						return this.dbc//
								.sql("update stb_spring_tip_bites set promoted = NOW() where uid = :uid ")//
								.bind("uid", springTip.uid())//
								.fetch()//
								.rowsUpdated()//
								.map(count -> true);
					}
					return Mono.just(false);
				})//
				.filter(res -> (res))//
				.map(r -> springTip)//
				.switchIfEmpty(Mono.error(new IllegalArgumentException("can't promote [" + springTip + "]")));//
		this.tx.transactional(promotionPipeline) //
				.subscribe(s -> log.info("promoted [" + s + "]"));
	}

}
