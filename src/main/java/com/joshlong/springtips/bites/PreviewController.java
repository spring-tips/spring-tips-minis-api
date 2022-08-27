package com.joshlong.springtips.bites;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

// todo secure this endpoint
@Controller
@Slf4j
@ResponseBody
@RequiredArgsConstructor
class PreviewController {

	private final TipManifestReader tipManifestReader;

	private final Renderer renderer;

	@PostMapping("/tips/preview")
	ResponseEntity<?> preview(@RequestBody MultipartFile tip) throws Exception {
		var inputStream = tip.getInputStream();
		var inputStreamResource = new InputStreamResource(inputStream);
		var xml = ResourceUtils.read(inputStreamResource);
		var tipObject = this.tipManifestReader.read("preview", xml);
		log.info("got the following response: " + tipObject);
		var svgXMl = this.renderer.createSvgDocument(tipObject.title(), tipObject.code());
		var jpgImage = this.renderer.transcodeSvgDocument(svgXMl, Renderer.Extension.JPG);
		var jpgImageResource = new ByteArrayResource(jpgImage);
		return ResponseEntity.ok() //
				.contentType(MediaType.IMAGE_JPEG) //
				.header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*") //
				.body(jpgImageResource);
	}

}
