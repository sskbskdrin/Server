package parse;

import org.xml.sax.Attributes;

public interface StartElementListener {
    void start(Attributes attributes);
}
