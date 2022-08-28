package com.joshlong.springtips.bites;

import org.springframework.util.Assert;

record SpringTip(String code, String title, String tweet, String uid) {

	SpringTip(String code, String title, String tweet, String uid) {

		Assert.hasText(code, () -> "the code is null");
		Assert.hasText(uid, () -> "the uid is null");
		Assert.hasText(title, () -> "the title is null");
		Assert.hasText(tweet, () -> "the tweetJson is null");
		this.code = code;
		this.uid = uid.trim();
		this.title = title.trim();
		this.tweet = tweet;
	}
}
