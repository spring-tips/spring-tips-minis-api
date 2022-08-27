package com.joshlong.springtips.bites;

import java.io.File;
import java.util.Objects;

/**
 * Handles reading information from a validly formed {@code  tip.xml} file
 */
class TipManifestReader {

	SpringTip read(String tipName, String xml) {
		var doc = Objects.requireNonNull(XmlUtils.parseDocument(xml));
		var title = doc.getElementsByTagName("title").item(0).getTextContent();
		var code = doc.getElementsByTagName("code").item(0).getTextContent().trim();
		var tweet = doc.getElementsByTagName("tweet").item(0).getTextContent();
		return new SpringTip(code, title, tipName, tweet);
	}

	SpringTip read(File file, String xml) {
		return read(file.getName(), xml);
	}

}
