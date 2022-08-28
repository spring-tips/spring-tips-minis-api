package com.joshlong.springtips.bites;

import java.io.File;
import java.util.Objects;

/**
 * Handles reading information from a validly formed {@code  tip.xml} file
 */
class TipManifestReader {

	private static String trim(String t) {
		return t == null ? "" : (t.trim().stripLeading().stripTrailing().stripIndent());
	}

	SpringTip read(String tipName, String xml) {
		var doc = Objects.requireNonNull(XmlUtils.parseDocument(xml));
		var title = trim(doc.getElementsByTagName("title").item(0).getTextContent());
		var uid = trim(doc.getElementsByTagName("uid").item(0).getTextContent());
		var code = (doc.getElementsByTagName("code").item(0).getTextContent());
		var tweet = trim(doc.getElementsByTagName("tweet").item(0).getTextContent());
		return new SpringTip(code, title, tipName, tweet, uid);
	}

	SpringTip read(File file, String xml) {
		return read(file.getName(), xml);
	}

}
