package forestry.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// the game dropped its own annotation in 1.21.5, ForestryGameTests turns this one into a test instance
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
	String template() default "";

	int timeoutTicks() default 100;
}
