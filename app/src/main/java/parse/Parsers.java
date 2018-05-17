package parse;

import java.io.IOException;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * @author ex-keayuan001
 */
public class Parsers {
    public static void parse(Reader in, ContentHandler contentHandler) throws SAXException, IOException {
        try {
            XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            reader.setFeature("http://xml.org/sax/features/namespaces", true);
            reader.setContentHandler(contentHandler);
            reader.parse(new InputSource(in));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
