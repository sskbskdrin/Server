package parse;

/* compiled from: Children */
class Children {
    Child[] f12a = new Child[16];

    /* compiled from: Children */
    static class Child extends Element {
        final int f10a;
        Child f11b;

        Child(Element parent, String uri, String localName, int depth, int hash) {
            super(parent, uri, localName, depth);
            this.f10a = hash;
        }
    }

    Children() {
    }

    Element m12a(Element parent, String uri, String localName) {
        int hash = (uri.hashCode() * 31) + localName.hashCode();
        int index = hash & 15;
        Child current = this.f12a[index];
        if (current == null) {
            current = new Child(parent, uri, localName, parent.f2e + 1, hash);
            this.f12a[index] = current;
            return current;
        }
        Child previous;
        do {
            if (current.f10a == hash && current.f0c.compareTo(uri) == 0 && current.f1d.compareTo(localName) == 0) {
                return current;
            }
            previous = current;
            current = current.f11b;
        } while (current != null);
        current = new Child(parent, uri, localName, parent.f2e + 1, hash);
        previous.f11b = current;
        return current;
    }

    Element m13a(String uri, String localName) {
        int hash = (uri.hashCode() * 31) + localName.hashCode();
        Child current = this.f12a[hash & 15];
        if (current == null) {
            return null;
        }
        do {
            if (current.f10a == hash && current.f0c.compareTo(uri) == 0 && current.f1d.compareTo(localName) == 0) {
                return current;
            }
            current = current.f11b;
        } while (current != null);
        return null;
    }
}
