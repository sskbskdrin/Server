package parse;

import java.util.ArrayList;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

/**
 * @author ex-keayuan001
 */
public class Element {
    final String f0c;
    final String f1d;
    final int f2e;
    final Element f3f;
    Children f4g;
    ArrayList<Element> f5h;
    boolean f6i;
    StartElementListener f7j;
    EndElementListener f8k;
    EndTextElementListener f9l;

    Element(Element parent, String uri, String localName, int depth) {
        this.f3f = parent;
        this.f0c = uri;
        this.f1d = localName;
        this.f2e = depth;
    }

    public Element m1a(String localName) {
        return m2a("", localName);
    }

    public Element m2a(String uri, String localName) {
        if (this.f9l != null) {
            throw new IllegalStateException("This element already has an end text element listener. It cannot have "
                + "children.");
        }
        if (this.f4g == null) {
            this.f4g = new Children();
        }
        return this.f4g.m12a(this, uri, localName);
    }

    public Element m10b(String localName) {
        return m11b("", localName);
    }

    public Element m11b(String uri, String localName) {
        Element child = m2a(uri, localName);
        if (this.f5h == null) {
            this.f5h = new ArrayList();
            this.f5h.add(child);
        } else if (!this.f5h.contains(child)) {
            this.f5h.add(child);
        }
        return child;
    }

    public void m4a(ElementListener elementListener) {
        m7a((StartElementListener) elementListener);
        m5a((EndElementListener) elementListener);
    }

    public void m8a(TextElementListener elementListener) {
        m7a((StartElementListener) elementListener);
        m6a((EndTextElementListener) elementListener);
    }

    public void m7a(StartElementListener startElementListener) {
        if (this.f7j != null) {
            throw new IllegalStateException("Start element listener has already been set.");
        }
        this.f7j = startElementListener;
    }

    public void m5a(EndElementListener endElementListener) {
        if (this.f8k != null) {
            throw new IllegalStateException("End element listener has already been set.");
        }
        this.f8k = endElementListener;
    }

    public void m6a(EndTextElementListener endTextElementListener) {
        if (this.f9l != null) {
            throw new IllegalStateException("End text element listener has already been set.");
        } else if (this.f4g != null) {
            throw new IllegalStateException("This element already has children. It cannot have an end text element "
                + "listener.");
        } else {
            this.f9l = endTextElementListener;
        }
    }

    public String toString() {
        return Element.m0c(this.f0c, this.f1d);
    }

    static String m0c(String uri, String localName) {
        StringBuilder append = new StringBuilder().append("'");
        if (!uri.equals("")) {
            localName = uri + ":" + localName;
        }
        return append.append(localName).append("'").toString();
    }

    void m3a() {
        ArrayList<Element> requiredChildren = this.f5h;
        if (requiredChildren != null) {
            for (int i = requiredChildren.size() - 1; i >= 0; i--) {
                ((Element) requiredChildren.get(i)).f6i = false;
            }
        }
    }

    void m9a(Locator locator) throws SAXParseException {
        ArrayList<Element> requiredChildren = this.f5h;
        if (requiredChildren != null) {
            int i = requiredChildren.size() - 1;
            while (i >= 0) {
                Element child = (Element) requiredChildren.get(i);
                if (child.f6i) {
                    i--;
                } else {
                    throw new BadXmlException("Element named " + this + " is missing required" + " child element " +
                        "named " + child + ".", locator);
                }
            }
        }
    }
}
