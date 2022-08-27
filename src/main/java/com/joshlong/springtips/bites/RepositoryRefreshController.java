package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Instant;
import java.util.Objects;

@Controller
@ResponseBody
@RequiredArgsConstructor
class RepositoryRefreshController {

	private final String indexRebuildKey;

	private final ApplicationEventPublisher publisher;

	/*
	 * this is for github
	 */
	@PostMapping("/index")
	ResponseEntity<?> refresh(RequestEntity<String> requestEntity) throws Exception {
		var theirHash = HmacUtils.generateHmac256(Objects.requireNonNull(requestEntity.getBody()),
				this.indexRebuildKey.getBytes());
		var myHash = getGithubWebhookRequestSha256HeaderValue(requestEntity);
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
