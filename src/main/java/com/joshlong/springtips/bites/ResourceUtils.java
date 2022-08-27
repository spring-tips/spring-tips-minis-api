package com.joshlong.springtips.bites;

import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;

abstract class ResourceUtils {

	@SneakyThrows
	static void write(String str, Resource resource) {
		if (resource instanceof WritableResource writableResource) {
			try (var in = new StringReader(str); var out = new OutputStreamWriter(writableResource.getOutputStream())) {
				FileCopyUtils.copy(in, out);
			}
		}
	}

	@SneakyThrows
	static void write(byte[] str, Resource resource) {
		if (resource instanceof WritableResource writableResource) {
			try (var in = new ByteArrayInputStream(str); var out = writableResource.getOutputStream()) {
				FileCopyUtils.copy(in, out);
			}
		}
	}

	@SneakyThrows
	static String read(Resource resource) {

		try (var in = resource.getInputStream(); var reader = new InputStreamReader(in)) {
			return FileCopyUtils.copyToString(reader);
		}
	}

}
