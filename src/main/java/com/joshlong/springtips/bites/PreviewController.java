package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;

@Controller
@ResponseBody
@RequiredArgsConstructor
class PreviewController {

	private final TipManifestReader tipManifestReader;

	private final Renderer renderer;

	@PostMapping(value = "/tips/preview", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
			produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseStatus(value = HttpStatus.OK)
	Resource preview(@RequestBody byte[] tip) {
		return buildPreviewFromXmlInput(this.tipManifestReader, this.renderer, tip);
	}

	@GetMapping("/hello")
	String hello(Authentication authentication) {
		return "Hello, " + authentication.getName();
	}

	@SneakyThrows
	private static Resource buildPreviewFromXmlInput(TipManifestReader tipManifestReader, Renderer renderer,
			byte[] bytes) {
		try (var in = new ByteArrayInputStream(bytes)) {
			var xml = ResourceUtils.read(new InputStreamResource(in));
			var tipObject = tipManifestReader.read(xml);
			var svgXMl = renderer.createSvgDocument(tipObject.title(), tipObject.code());
			var jpgImage = renderer.transcodeSvgDocument(svgXMl, Renderer.Extension.PNG);
			return new ByteArrayResource(jpgImage);
		}
	}

}
