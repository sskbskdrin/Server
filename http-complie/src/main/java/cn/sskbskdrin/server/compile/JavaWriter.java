package cn.sskbskdrin.server.compile;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.Modifier;

/**
 * Created by keayuan on 2020/9/9.
 *
 * @author keayuan
 */
public final class JavaWriter implements Closeable {

    private static final Pattern TYPE_PATTERN = Pattern.compile("(?:[\\w$]+\\.)*([\\w\\.*$]+)");
    static String indent = "    ";
    private final Writer out;

    public JavaWriter(Writer out, Type<FileType> type) {
        this.out = out;
        FileType fileType = new FileType(out);
        type.apply(fileType);
    }

    private static boolean isNotEmpty(String text) {
        return text != null && text.length() > 0;
    }

    private static boolean isNotEmpty(Collection collection) {
        return collection != null && !collection.isEmpty();
    }

    public static <T> List<T> list(T... t) {
        return Arrays.asList(t);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    public static class BlockType extends BaseType {

        private BlockType(BaseType parent, boolean isStatic) {
            super(parent);
            append('\n').append(parent.tab);
            if (isStatic) {
                append("static ");
            }
            blockStart();
        }
    }

    public static class MethodType extends BaseType {

        private MethodType(BaseType parent, String name, String back, Set<Modifier> modifiers,
                           List<String> parameters, Set<String> throwsTypes) {
            super(parent);
            append('\n').append(parent.tab).modifiers(modifiers);
            if (isNotEmpty(back)) {
                append(back).append(' ');
            }
            append(name).append('(');
            if (isNotEmpty(parameters)) {
                appendParams(parameters);
            }
            append(')').append(' ');
            if (isNotEmpty(throwsTypes)) {
                append("throw ");
                append(throwsTypes);
            }
            blockStart();
        }
    }

    public static class ClassType extends BaseType {

        private ClassType(BaseType parent, String name, Set<Modifier> modifiers, String extend,
                          Set<String> interfaces) {
            super(parent);
            append('\n').append(parent.tab).modifiers(modifiers).append("class").append(' ').append(name).append(' ');
            if (extend != null && extend.length() > 0) {
                append("extends ").append(extend).append(' ');
            }
            if (interfaces != null && interfaces.size() > 0) {
                append("implements ");
                append(interfaces);
            }
            blockStart();
        }

        public ClassType block(boolean isStatic, Type<BlockType> block) {
            BlockType type = new BlockType(this, isStatic);
            block.apply(type);
            append(tab).blockEnd();
            return this;
        }

        public ClassType field(String type, String name) {
            return field(type, name, EnumSet.noneOf(Modifier.class), null);
        }

        public ClassType field(String type, String name, Set<Modifier> modifiers) {
            return field(type, name, modifiers, null);
        }

        public ClassType field(String type, String name, Set<Modifier> modifiers, String init) {
            append(tab).modifiers(modifiers).append(type).append(' ').append(name);
            if (init != null && init.length() > 0) {
                append(" = ").append(init);
            }
            end();
            return this;
        }

        public ClassType methodAbstract(String returnType, String name, Set<Modifier> modifiers,
                                        List<String> parameters) {
            if (modifiers == null) {
                modifiers = EnumSet.of(Modifier.ABSTRACT);
            }
            if (!modifiers.contains(Modifier.ABSTRACT)) {
                modifiers = EnumSet.of(Modifier.ABSTRACT, modifiers.toArray(new Modifier[0]));
            }
            append(tab).modifiers(modifiers).append(returnType).append(' ').append(name);
            append('(').appendParams(parameters).append(")").end();
            return this;
        }

        public ClassType method(String returnType, String name, Type<MethodType> type) {
            return method(returnType, name, null, null, null, type);
        }

        public ClassType method(String returnType, String name, Set<Modifier> modifiers, Type<MethodType> type) {
            return method(returnType, name, modifiers, null, null, type);
        }

        public ClassType method(String returnType, String name, Set<Modifier> modifiers, List<String> parameters,
                                Type<MethodType> type) {
            return method(returnType, name, modifiers, parameters, null, type);
        }

        public ClassType method(String returnType, String name, Set<Modifier> modifiers, List<String> parameters,
                                Set<String> throwsTypes, Type<MethodType> type) {
            return (ClassType) super.method(returnType, name, modifiers, parameters, throwsTypes, type);
        }
    }

    public static class InterfaceType extends BaseType {

        private InterfaceType(BaseType parent, String name, Set<Modifier> modifiers, Set<String> interfaces) {
            super(parent);
            append(parent.tab).modifiers(modifiers).append("interface ").append(name).append(interfaces).blockStart();
        }

        public InterfaceType method(String returnType, String name, String... params) {
            append(tab).append(returnType).append(' ').append(name);
            append('(').appendParams(Arrays.asList(params)).append(')').end();
            return this;
        }

        public InterfaceType methodDefault(String returnType, String name, List<String> params,
                                           Set<String> throwsTypes, Type<MethodType> type) {
            MethodType methodType = new MethodType(this, name, returnType, EnumSet.of(Modifier.DEFAULT), params,
                throwsTypes);
            if (type != null) {
                type.apply(methodType);
            }
            append(tab).blockEnd();
            return this;
        }
    }

    public static class FileType extends BaseType {

        private FileType(Writer out) {
            super(null);
            tab = "";
            writer = out;
        }

        public FileType emitPackage(String packageName) {
            if (isNotEmpty(packageName)) {
                append("package ").append(packageName);
                end();
            }
            return this;
        }

        public FileType emitClass(String name, Set<Modifier> modifiers, String extend, Set<String> interfaces,
                                  Type<ClassType> type) {
            ClassType classType = new ClassType(this, name, modifiers, extend, interfaces);
            if (type != null) {
                type.apply(classType);
            }
            append(tab).blockEnd();
            return this;
        }

        public FileType emitInterface(String name, Set<Modifier> modifiers, Set<String> interfaces,
                                      Type<InterfaceType> type) {

            InterfaceType interfaceType = new InterfaceType(this, name, modifiers, interfaces);
            if (type != null) {
                type.apply(interfaceType);
            }
            append(tab).blockEnd();
            return this;
        }

        /**
         * Emit an import for each {@code type} provided. For the duration of the file, all references to
         * these classes will be automatically shortened.
         */
        public FileType emitImports(String... types) {
            return emitImports(Arrays.asList(types));
        }

        /**
         * Emit an import for each {@code type} in the provided {@code Collection}. For the duration of
         * the file, all references to these classes will be automatically shortened.
         */
        public FileType emitImports(Collection<String> types) {
            append('\n');
            for (String type : types) {
                Matcher matcher = TYPE_PATTERN.matcher(type);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException(type);
                }
                append("import ").append(type).end();
            }
            return this;
        }
    }

    public static class FlowType extends BaseType {
        private BaseType parent;

        private FlowType(BaseType parent, String pattern) {
            super(parent);
            this.parent = parent;
            append('\n').append(parent.tab).append(pattern).append(' ').blockStart();
        }

        public FlowType nextFlow(String pattern, Type<FlowType> type) {
            append(parent.tab).append('}').append(' ').append(pattern).append(' ').blockStart();
            if (type != null) {
                type.apply(this);
            }
            return this;
        }

        @Override
        public FlowType flow(String pattern, Type<FlowType> type) {
            return super.flow(pattern, type);
        }

        public void endFlow() {
            append(parent.tab).blockEnd();
        }

        public void endFlow(String pattern) {
            append(parent.tab).append('}').append(' ').append(pattern).append('\n');
        }
    }

    private static class BaseType {
        protected Writer writer;
        protected String tab;

        private BaseType(BaseType parent) {
            tab = (parent == null ? "" : parent.tab) + indent;
            if (parent != null) {
                writer = parent.writer;
            }
        }

        private BaseType modifiers(Set<Modifier> modifiers) {
            if (isNotEmpty(modifiers)) {
                for (Modifier modifier : modifiers) {
                    append(modifier.toString()).append(' ');
                }
            }
            return this;
        }

        public BaseType statement(String pattern, Object... args) {
            append(tab).append(String.format(pattern, args));
            end();
            return this;
        }

        public BaseType comment(String content) {
            append(tab).append("// ").append(content).append('\n');
            return this;
        }

        protected FlowType flow(String pattern, Type<FlowType> type) {
            FlowType flow = new FlowType(this, pattern);
            if (type != null) {
                type.apply(flow);
            }
            return flow;
        }

        protected void end() {
            append(";\n");
        }

        protected void blockStart() {
            append("{\n");
        }

        protected void blockEnd() {
            append("}\n");
        }


        protected BaseType append(char c) {
            try {
                writer.append(c);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        protected BaseType append(String c) {
            try {
                writer.append(c);
            } catch (IOException ignored) {
            }
            return this;
        }

        protected BaseType append(Collection<String> set) {
            Iterator<String> iterator = set.iterator();
            boolean first = true;
            while (iterator.hasNext()) {
                if (!first) {
                    append(", ");
                }
                append(iterator.next());
                first = false;
            }
            return this;
        }

        protected BaseType appendParams(List<String> params) {
            if (isNotEmpty(params)) {
                boolean isFirst = true;
                for (int i = 0; i < params.size(); ) {
                    if (!isFirst) append(", ");
                    append(params.get(i++)).append(' ').append(params.get(i++));
                    isFirst = false;
                }
            }
            return this;
        }

        protected BaseType method(String returnType, String name, Set<Modifier> modifiers, List<String> parameters,
                                  Set<String> throwsTypes, Type<MethodType> type) {
            MethodType methodType = new MethodType(this, name, returnType, modifiers, parameters, throwsTypes);
            if (type != null) {
                type.apply(methodType);
            }
            append(tab).blockEnd();
            return this;
        }
    }

    public interface Type<T> {
        void apply(T t);
    }
}
