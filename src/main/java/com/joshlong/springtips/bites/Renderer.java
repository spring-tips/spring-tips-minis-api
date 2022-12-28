package com.joshlong.springtips.bites;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Renders an image driven by an SVG template.
 *
 * @author Josh Long
 */
@Slf4j
class Renderer {

	private final Resource templateResource = new ClassPathResource("/template.svg");

	public Resource render(SpringTip tipObject) {
		var svgXMl = this.createSvgDocument(tipObject.title(), tipObject.code());
		var jpgImage = this.transcodeSvgDocument(svgXMl, Renderer.Extension.PNG);
		return new ByteArrayResource(jpgImage);
	}

	@SneakyThrows
	public String createSvgDocument(String title, String code) {

		var list = Arrays//
				.stream(code.split(System.lineSeparator()))//
				.map(Renderer::xmlEncodeLine) //
				.map(l -> l.stripTrailing() + System.lineSeparator())//
				.toList();

		var colorizedCode = stylizeCode(list);

		var document = XmlUtils.parseDocument(this.templateResource);

		// title
		replaceContent(document, "tspan", "titleText", title);

		// code
		replaceContent(document, "text", "codeText", colorizedCode);

		// set the font size based on how many lines of code we have
		var codeText = XmlUtils.getElementById(document, "text", "codeText");
		var fontSize = calculateFontSizeForLines(list) + "";
		Objects.requireNonNull(codeText).getAttributes().getNamedItem("font-size").setNodeValue(fontSize);

		// render
		var xml = XmlUtils.renderDocument(document);
		for (var i = 0; i < 2; i++)
			xml = fixSuccessiveTspanElementError(xml);
		return xml;
	}

	private static String fixSuccessiveTspanElementError(String xml) {
		var lines = xml.split(System.lineSeparator());
		var nl = new ArrayList<String>();
		for (var i = 0; i < lines.length; i++) {
			var current = lines[i];
			var next = ((i + 1) < lines.length) ? lines[i + 1] : " ";
			if (current.trim().endsWith(">") && next.startsWith("<tspan")) {
				nl.add(current.trim() + "" + next.trim());
				i += 1;
			} //
			else {
				nl.add(current.trim());
			}
		}
		return nl.stream().collect(Collectors.joining(System.lineSeparator()));
	}

	public enum Extension {

		JPG, PNG

	}

	public byte[] transcodeSvgDocument(String xml, Extension extension) {
		var ext = extension == null ? null : extension.name().toLowerCase(Locale.ROOT);
		return transcodeSvgAsImage(xml, ext);
	}

	/**
	 * Valid values for {@code ext} are {@code  jpg}, {@code png}
	 */
	@SneakyThrows
	private static byte[] transcodeSvgAsImage(String xml, @Nullable String ext) {
		var transcoder = (ImageTranscoder) ((!StringUtils.hasText(ext) || ext.toLowerCase(Locale.ROOT).equals("jpg"))
				? jpegTranscoder() : pngTranscoder());
		try (var in = new ByteArrayInputStream(xml.getBytes()); var out = new ByteArrayOutputStream()) {
			var input = new TranscoderInput(in);
			var output = new TranscoderOutput(out);
			transcoder.transcode(input, output);
			return (out.toByteArray());
		}
	}

	private static String xmlEncodeLine(String line) {
		/*
		 * it's important that we encode '&' first, otherwise we'll end up encoding the
		 * escape characters themselves so this code uses a LinkedHashMap
		 */
		var later = Map.of(//
				"<", "&lt;", //
				">", "&gt;", //
				"'", "&apos;", //
				"\"", "&quot;" //
		);
		var invalid = new LinkedHashMap<String, String>();
		invalid.put("&", "&amp;");
		invalid.putAll(later);
		for (var e : invalid.entrySet())
			line = line.replaceAll(e.getKey(), e.getValue());
		return line;
	}

	private static int calculateFontSizeForLines(List<String> lines) {
		var lineCount = lines.size();
		var mapping = Map.of(// map( lines => font size)
				10, 40, //
				20, 30, //
				30, 30 //
		);
		var match = mapping.keySet() //
				.stream()//
				.sorted(Comparator.reverseOrder())//
				.filter(threshold -> lineCount >= threshold) //
				.map(mapping::get) //
				.toList();
		if (match.size() > 0)
			return match.get(0);
		return mapping.get(10);
	}

	private static String stylizeCode(String line) {
		var syntaxColor = "#333399";
		var tokens = new HashMap<String, String>();
		tokens.putAll(Map.of(//
				"public", syntaxColor, //
				"class", syntaxColor, //
				"final", syntaxColor, //
				"var", syntaxColor, //
				"private", syntaxColor, //
				"implements", syntaxColor //
		)); // there's a limit to the Map.of() arity so i use multiple maps
		tokens.putAll(Map.of(//
				"synchronized", syntaxColor, //
				"fun", syntaxColor, //
				"def", syntaxColor, //
				"suspend", syntaxColor, //
				"package", syntaxColor, //
				"new", syntaxColor, //
				"this", syntaxColor //
		));
		for (var e : tokens.entrySet()) {
			var token = e.getKey();
			var color = e.getValue();
			line = line.replaceAll(token, "" + String.format(trim("""
					<tspan fill="%s">%s</tspan>
					"""), color, token) + "");
		}
		return line;
	}

	private static String repeat(int count) {
		var s = new StringBuilder();
		s.append(" ".repeat(Math.max(0, count)));
		return s.toString();
	}

	private static String stylizeCode(List<String> lines) {
		var tspanTemplate = trim("""
				 <tspan x="100" dy="1em">%s</tspan>
				""");
		return lines.stream() //
				.map(l -> {
					var wsCount = 0;
					for (var c : l.toCharArray()) {
						if (c == ' ')
							wsCount += 1;
						else
							break;
					}

					if (trim(l).startsWith("//"))
						return repeat(wsCount) + "<tspan class = \"comment\">" + l.trim().stripLeading() + "</tspan>";
					return stylizeCode(l);
				}).map(line -> String.format(tspanTemplate, line))//
				.collect(Collectors.joining());
	}

	private static void replaceContent(Document document, String tagName, String elementId, String newCode) {
		var codeNode = Objects.requireNonNull(XmlUtils.getElementById(document, tagName, elementId));
		var newTspans = XmlUtils.parseDocument("<code>" + newCode + "</code>");
		var nl = Objects.requireNonNull(newTspans).getDocumentElement().getChildNodes();
		for (var ctr = 0; ctr < nl.getLength(); ctr++) {
			var tNode = document.importNode(nl.item(ctr), true);
			codeNode.appendChild(tNode);
		}
	}

	private static String trim(String string) {
		Assert.notNull(string, "can't trim a null-String");
		return string.stripTrailing().stripLeading().stripIndent().trim();
	}

	private static JPEGTranscoder jpegTranscoder() {
		var transcoder = new JPEGTranscoder();
		transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, 1F);
		return transcoder;
	}

	private static PNGTranscoder pngTranscoder() {
		return new PNGTranscoder();
	}

}
