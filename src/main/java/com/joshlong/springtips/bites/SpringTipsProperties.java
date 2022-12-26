package com.joshlong.springtips.bites;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.URI;

@ConfigurationProperties(value = "springtips")
record SpringTipsProperties(Fonts fonts, Twitter twitter, File outputDirectory, Github github) {

	record Encryption(String salt, String password) {
	}

	record Fonts(Encryption encryption, Resource archive) {
	}

	record Github(URI gitRepository, File cloneDirectory, String rebuildKey) {
	}

	record Twitter(String username, Client client) {

		record Client(String id, String secret) {
		}
	}

}
