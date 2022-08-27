package com.joshlong.springtips.bites;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;

@Slf4j
class XmlUtilsTest {

	@Test
	void dom() throws Exception {
		var xmlString = """
				   <p>
				       hello
				       <span>world</span>
				       and
				       <span>welcome</span>
				   </p>
				""";
		var parts = XmlUtils.parse(xmlString);
		var dbf = DocumentBuilderFactory.newInstance();
		var dom = dbf.newDocumentBuilder();
		var doc = dom.newDocument();
		var rootElement = doc.createElement("root");
		doc.appendChild(rootElement);
		var root = doc.getDocumentElement();
		for (var part : parts) {
			var copyNode = doc.importNode(part, true);
			root.appendChild(copyNode);
		}
		var xml = XmlUtils.renderDocument(doc);
		Assertions.assertTrue(xml.contains("<p>") && xml.contains("</p>"));
	}

}