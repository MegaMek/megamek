package megamek.utils;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MegaMekXmlUtil {
    private static DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;
    private static SAXParserFactory SAX_PARSER_FACTORY;

	/**
	 * Creates a DocumentBuilder safe from XML external entities
	 * attacks, and XML entity expansion attacks.
	 * @return A DocumentBuilder safe to use to read untrusted XML.
	 */
	public static DocumentBuilder newSafeDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY;
		if (null == dbf) {
			// At worst we may do this twice if multiple threads
			// hit this method. It is Ok to have more than one
			// instance of the builder factory, as long as it is
			// XXE safe.
			dbf = DocumentBuilderFactory.newInstance();

			//
			// Adapted from: https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXP_DocumentBuilderFactory.2C_SAXParserFactory_and_DOM4J
			//
			// "...The JAXP DocumentBuilderFactory setFeature method allows a
			// developer to control which implementation-specific XML processor
			// features are enabled or disabled. The features can either be set
			// on the factory or the underlying XMLReader setFeature method. 
			// Each XML processor implementation has its own features that 
			// govern how DTDs and external entities are processed."
			//
			// "[disable] these as well, per Timothy Morgan's 2014 paper: 'XML 
			// Schema, DTD, and Entity Attacks'"
			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);

			// "This is the PRIMARY defense. If DTDs (doctypes) are disallowed,
			// almost all XML entity attacks are prevented"
			String FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
			dbf.setFeature(FEATURE, true);

			DOCUMENT_BUILDER_FACTORY = dbf;
		}

		return dbf.newDocumentBuilder();
	}

    /**
     * Creates a JAXB compatible Source safe from XML external entities
     * attacks, and XML entity expansion attacks.
     * @return A Source safe to use to read untrusted XML from a JAXB unmarshaller.
     */
	public static Source createSafeXmlSource(InputStream inputStream) throws SAXException, ParserConfigurationException {
		SAXParserFactory spf = SAX_PARSER_FACTORY;
		if (null == spf) {
			// At worst we may do this twice if multiple threads
			// hit this method. It is Ok to have more than one
			// instance of the parser factory, as long as it is
			// XXE safe.
			spf = SAXParserFactory.newInstance();

			//
			// Adapted from: https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#JAXB_Unmarshaller
			//
			// "Since a javax.xml.bind.Unmarshaller parses XML and does not
			// support any flags for disabling XXE, it’s imperative to parse 
			// the untrusted XML through a configurable secure parser first, 
			// generate a source object as a result, and pass the source
			// object to the Unmarshaller."
			//
			spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

			SAX_PARSER_FACTORY = spf;
		}

		return new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(inputStream));
	}
}
