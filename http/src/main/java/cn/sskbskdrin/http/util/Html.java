package cn.sskbskdrin.http.util;

/**
 * Created by keayuan on 2020/9/8.
 *
 * @author keayuan
 */
public class Html {
    HtmlInner inner;
    static String indent = "  ";

    public Html() {
        inner = new HtmlInner();
    }

    public Html header(Content<Header> run) {
        Header module = new Header(inner);
        run.apply(module);
        module.end();
        return this;
    }

    public Html body(Content<Div> run) {
        Div module = Div.getBody(inner);
        run.apply(module);
        module.end();
        return this;
    }

    @Override
    public String toString() {
        inner.end();
        return inner.toString();
    }

    private static class HtmlInner extends Label {
        HtmlInner() {
            super(null, "html");
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }

    public static class Header extends Label {
        Header(Label label) {
            super(label, "head");
        }

        public void meta(String text) {
            label("meta", "", text.split("="));
        }

        public void title(String text) {
            label("title", text);
        }
    }

    public static class Div extends Label {

        Div(Label label) {
            this(label, false);
        }

        private Div(Label label, boolean isBody) {
            super(label, isBody ? "body" : "div");
        }

        private static Div getBody(HtmlInner inner) {
            return new Div(inner, true);
        }

        public void h1(String text) {
            h(text, 1);
        }

        public void h2(String text) {
            h(text, 2);
        }

        public void h3(String text) {
            h(text, 3);
        }

        public void h4(String text) {
            h(text, 4);
        }

        public void h5(String text) {
            h(text, 5);
        }

        public void h6(String text) {
            h(text, 6);
        }

        public void p(String text) {
            label("p", text);
        }

        public void a(String text, String href, String target) {
            label("a", text, "href", href, "target", target);
        }

        public void text(String text) {
            builder.append(text);
        }

        public void input(String type, String name) {
            label("input", "", "type", type, "name", name);
        }

        public void br() {
            append("</br>");
        }

        public void form(Content<Form> module, String action, String enctype, String method) {
            Form form = new Form(this, action, enctype, method);
            module.apply(form);
            form.end();
        }

        private void h(String text, int n) {
            label("h" + n, text);
        }

        public void div(Content<Div> module) {
            Div div = new Div(this);
            module.apply(div);
            div.end();
        }
    }

    public static class Form extends Label {

        Form(Label label, String action, String enctype, String method) {
            super(label, "form", "action", action, "enctype", enctype, "method", method);
        }

        public void input(String type, String name) {
            label("input", "", "type", type, "name", name);
        }
    }

    private static class Label {
        StringBuilder builder;
        String name;
        String tab = "";
        String[] attrs;

        Label(Label label, String name, String... attrs) {
            this.attrs = attrs;
            if (label == null) {
                builder = new StringBuilder();
                line();
                append("<!DOCTYPE html>");
            } else {
                this.builder = label.builder;
                tab = label.tab + indent;
            }
            this.name = name;
            start();
        }

        void label(String name, String content, String... attrs) {
            builder.append(tab).append(indent).append('<').append(name);
            if (attrs != null && attrs.length > 0) {
                for (int i = 0; i < attrs.length; ) {
                    builder.append(' ').append(attrs[i++]).append('=').append('"').append(attrs[i++]).append('"');
                }
            }
            builder.append('>');
            builder.append(content);
            builder.append("</").append(name).append('>');
            line();
        }

        void append(String text) {
            builder.append(tab).append(text);
        }

        void start() {
            builder.append(tab).append('<').append(name);
            if (attrs != null && attrs.length > 0) {
                for (int i = 0; i < attrs.length; ) {
                    builder.append(' ').append(attrs[i++]).append('=').append('"').append(attrs[i++]).append('"');
                }
            }
            builder.append('>');
            line();
        }

        void line() {
            builder.append('\n');
        }

        void end() {
            builder.append(tab).append("</").append(name).append('>');
            line();
        }
    }

    public interface Content<T extends Label> {
        void apply(T t);
    }
}
