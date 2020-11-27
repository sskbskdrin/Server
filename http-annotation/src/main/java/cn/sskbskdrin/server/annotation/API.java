package cn.sskbskdrin.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by keayuan on 2020/9/9.
 *
 * @author keayuan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface API {
    String value();
}
