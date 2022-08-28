package com.joshlong.springtips.bites;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Controller
@ResponseBody
class RepositoryRefreshController {

	private final String rebuildKey;

	private final ApplicationEventPublisher publisher;

	RepositoryRefreshController(String rebuildKey, ApplicationEventPublisher publisher) {
		this.rebuildKey = rebuildKey;
		this.publisher = publisher;
		Assert.hasText(this.rebuildKey, "the rebuildKey must not be null");
	}

	/*
	 * this is for github
	 */
	@PostMapping("/refresh")
	ResponseEntity<?> refresh(RequestEntity<String> requestEntity) throws Exception {
		var theirHash = HmacUtils.generateHmac256(Objects.requireNonNull(requestEntity.getBody()),
				this.rebuildKey.getBytes());
		log.debug("theirHash " + theirHash);
		var myHash = getGithubWebhookRequestSha256HeaderValue(requestEntity);
		log.debug("myHash " + myHash);
		if (StringUtils.hasText(myHash) && StringUtils.hasText(theirHash)) {
			if (myHash.contains(theirHash)) {
				var event = new RepositoryRefreshEvent(Instant.now());
				this.publisher.publishEvent(event);
				return ResponseEntity.ok(event);
			}
		}
		return ResponseEntity.badRequest().build();
	}

	private static String getGithubWebhookRequestSha256HeaderValue(RequestEntity<String> requestEntity) {
		var headers = requestEntity.getHeaders();
		var headerKey = "X-Hub-Signature-256";
		if (headers.containsKey(headerKey)) {
			var strings = headers.get(headerKey);
			if (Objects.requireNonNull(strings).size() > 0) {
				return strings.get(0).trim();
			}
		}
		return null;
	}

}
