package parse;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

class BadXmlException extends SAXParseException {
    public BadXmlException(String message, Locator locator) {
        super(message, locator);
    }

    @Override
    public String getMessage() {
        return "Line " + getLineNumber() + ": " + super.getMessage();
    }
}
