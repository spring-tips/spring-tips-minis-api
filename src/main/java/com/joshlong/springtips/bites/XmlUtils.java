package com.joshlong.springtips.bites;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
abstract class XmlUtils {

	static Node getElementById(Document document, String tagName, String id) {

		var nodes = document.getElementsByTagName(tagName);
		for (var ctr = 0; ctr < nodes.getLength(); ctr++) {
			var node = nodes.item(ctr);
			var attributes = node.getAttributes();
			var idAttribute = attributes.getNamedItem("id");
			if (idAttribute != null) {
				if (idAttribute.getNodeValue().equals(id))
					return node;
			}
		}
		return null;
	}

	@SneakyThrows
	static String renderDocument(Document doc) {
		var transformerFactory = TransformerFactory.newInstance();
		var transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		var source = new DOMSource(doc);
		try (var writer = new StringWriter()) {
			var result = new StreamResult(writer);
			transformer.transform(source, result);
			return writer.toString();
		}
	}

	@SneakyThrows
	static Document parseDocument(Resource resource) {
		var xml = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		return parseDocument(xml);
	}

	static Document parseDocument(String xml) {
		try (var r = new StringReader(xml)) {
			var inputSource = new InputSource(r);
			var dbFactory = DocumentBuilderFactory.newInstance();
			var dBuilder = dbFactory.newDocumentBuilder();
			return dBuilder.parse(inputSource);
		} //
		catch (Exception ex) {
			log.error("couldn't parse the Document", ex);
		}
		return null;
	}

	@SneakyThrows
	static List<Node> parse(String xml) {
		try (var ir = new StringReader(xml)) {
			var dbf = DocumentBuilderFactory.newInstance();
			var dom = dbf.newDocumentBuilder();
			var doc = dom.parse(new InputSource(ir));
			var nl = doc.getChildNodes();
			var results = new ArrayList<Node>();
			for (var ctr = 0; ctr < nl.getLength(); ctr++) {
				results.add(nl.item(ctr));
			}
			return results;
		}
	}

}
