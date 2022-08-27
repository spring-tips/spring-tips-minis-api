package com.joshlong.springtips.bites;

import org.springframework.util.Assert;

record SpringTip(String code, String title, String name, String tweetJson) {

	SpringTip(String code, String title, String name, String tweetJson) {

		Assert.hasText(code, () -> "the code is null");
		Assert.hasText(title, () -> "the title is null");
		Assert.hasText(name, () -> "the name is null");
		Assert.hasText(tweetJson, () -> "the tweetJson is null");

		this.code = code;
		this.title = title.trim();
		this.name = name.trim();
		this.tweetJson = tweetJson;
	}
}
