package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Slf4j
@Controller
@ResponseBody
@RequiredArgsConstructor
class RenderController {

	private final TipManifestReader tipManifestReader;

	private final Renderer renderer;

	private final Repository repository;

	@ResponseStatus(HttpStatus.OK)
	@PostMapping(value = "/tips/renders", produces = MediaType.IMAGE_PNG_VALUE,
			consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	Resource read(@RequestBody byte[] tip) {
		return buildPreviewFromXmlInput(this.tipManifestReader, this.renderer, tip);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping(value = "/tips/renders/{id}", produces = MediaType.IMAGE_PNG_VALUE)
	Mono<Resource> read(@PathVariable Integer id) {
		log.info("the preview for [" + id + "]");
		var springTipMono = this.repository.findById(id);
		return springTipMono.map(this.renderer::render);
	}

	@GetMapping("/me")
	Map<String, String> hello(Authentication authentication) {
		return Map.of("greeting", "Hello, " + authentication.getName());
	}

	@SneakyThrows
	private static Resource buildPreviewFromXmlInput(TipManifestReader tipManifestReader, Renderer renderer,
			byte[] bytes) {
		try (var in = new ByteArrayInputStream(bytes)) {
			var xml = ResourceUtils.read(new InputStreamResource(in));
			var tipObject = tipManifestReader.read(xml);
			return renderer.render(tipObject);
		}
	}

}
