package cn.sskbskdrin.server.compile;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import cn.sskbskdrin.server.annotation.API;

public class CompileProcessor extends AbstractProcessor {

    private Messager messager;
    private Elements elementUtils;
    private Filer filer;
    private Types typeUtils;

    private void log(Object object) {
        messager.printMessage(Diagnostic.Kind.NOTE, object.toString());
    }

    /**
     * 初始化操作
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        typeUtils = processingEnvironment.getTypeUtils();
        elementUtils = processingEnvironment.getElementUtils();
        filer = processingEnvironment.getFiler();
        messager = processingEnvironment.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(API.class.getCanonicalName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        JavaWriter writer = null;
        JavaFileObject sourceFile = null;
        log("process");
        try {
            sourceFile = filer.createSourceFile("cn.sskbskdrin.server.http.Route");
            writer = new JavaWriter(sourceFile.openWriter(), fileType -> {
                fileType.comment("Generated code from http compile. Do not modify!");
                fileType.emitPackage("cn.sskbskdrin.server.http");
                fileType.emitImports("java.util.HashMap");
                fileType.emitClass("Route", EnumSet.of(Modifier.FINAL), null, null, classType -> {
                    classType.field("HashMap<String, Class<? extends HandlerServlet>>", "map",
                        EnumSet.of(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL), "new HashMap<>()");
                    classType.block(true, blockType -> {
                        for (Element element : roundEnv.getElementsAnnotatedWith(API.class)) {
                            blockType.statement("map.put(\"%s\", %s)", element.getAnnotation(API.class).value(), element
                                .asType()
                                .toString() + ".class");
                        }
                    });
                    classType.method("HandlerServlet", "route", EnumSet.of(Modifier.PUBLIC, Modifier.STATIC),
                        Arrays.asList("String", "path"), methodType -> {
                        methodType.flow("try", flowType -> {
                            flowType.statement("return map.containsKey(path) ? map.get(path).newInstance() : null");
                        }).nextFlow("catch (Exception ignored)", null).endFlow();
                        methodType.statement("return null");
                    });
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) writer.close();
                if (sourceFile != null) sourceFile.delete();
            } catch (IOException ignored) {
            }
        }
        return true;
    }
}
