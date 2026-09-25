package dev.eliasnvx.femboymod.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link FemboyAddon} implementation for discovery on NeoForge.
 *
 * <p>The annotated class must implement {@link FemboyAddon} and have a public no-argument
 * constructor. On Fabric this annotation is ignored; use the {@value FemboyAddon#FABRIC_ENTRYPOINT}
 * entrypoint instead.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterFemboyAddon {
}
