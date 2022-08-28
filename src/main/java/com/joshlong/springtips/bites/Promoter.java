package com.joshlong.springtips.bites;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joshlong.twitter.Twitter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

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

	private final ObjectMapper objectMapper;

	@SneakyThrows
	private String renderAndEncodeTweet(SpringTip springTip) {
		var map = Map.of("text", springTip.tweet());
		return objectMapper.writeValueAsString(map);
	}

	@EventListener
	public void scheduled(SpringTipsBiteScheduleTriggeredEvent event) {
		var st = event.getSource();
		var client = new Twitter.Client(this.twitterClientId, this.twitterClientSecret);
		var tweetJson = renderAndEncodeTweet(st);
		var promotionPipeline = this.twitter//
				.scheduleTweet(client, new Date(), this.twitterUsername, tweetJson) //
				.flatMap(result -> {
					if (result) {
						return this.dbc//
								.sql("update stb_spring_tip_bites set promoted = NOW () where uid = :uid ")//
								.bind("uid", st.uid())//
								.fetch()//
								.rowsUpdated()//
								.map(count -> true);
					}
					return Mono.just(false);
				})//
				.filter(res -> (res))//
				.map(r -> st)//
				.switchIfEmpty(Mono.error(new IllegalArgumentException("can't promote [" + st + "]")));//
		this.tx.transactional(promotionPipeline).subscribe(s -> log.info("promoted [" + s + "]"));
	}

}
