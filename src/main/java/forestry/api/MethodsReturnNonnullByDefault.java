package forestry.api;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks every method of the annotated package as returning a value that is never null.
 * Replaces the Minecraft annotation of the same name, which Minecraft 26.1 removed.
 */
@Nonnull
@TypeQualifierDefault(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodsReturnNonnullByDefault {
}
