package com.joshlong.springtips.bites;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.File;

@Slf4j
class CodeRendererTest {

	private final Renderer svgCodeRenderer = new Renderer();

	private final boolean open = false;

	private final File root = new File(System.getenv("HOME") + "/Desktop/outputDirectory");

	@Test
	void render() throws Exception {

		var titles = new String[] { //
				"Let's learn how to use Java Records", //
				"Use Spring Cloud OpenFeign to Build Declarative HTTP Clients", //
				"Using Spring Data R2DBC Repositories", //
				"Using Kotlin For Fun and Profit"//
		};

		Assert.state(this.root.exists() || this.root.mkdirs(),
				"the image directory [" + this.root.getAbsolutePath() + "] does not exist");

		for (var ctr = 0; ctr < titles.length; ctr++) {
			var title = titles[ctr];
			var codeInput = ResourceUtils.read(new ClassPathResource("code" + ctr + ".java"));
			Assert.hasText(codeInput, "the code must not be empty");
			var imageOutput = new File(this.root, "code" + ctr + ".jpg");
			var xmlOutput = new File(this.root, "code" + ctr + ".code");
			var xml = this.svgCodeRenderer.createSvgDocument(title, codeInput);
			var bytes = this.svgCodeRenderer.transcodeSvgDocument(xml, Renderer.Extension.JPG);
			FileCopyUtils.copy(bytes, imageOutput);
			FileCopyUtils.copy(xml.getBytes(), xmlOutput);
			log.debug(imageOutput.getAbsolutePath());
			if (this.open)
				Runtime.getRuntime().exec("open " + imageOutput.getAbsolutePath());

		}
	}

}
