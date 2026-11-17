package forestry.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// false keeps the class name out of the template path
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PrefixGameTestTemplate {
	boolean value() default true;
}
