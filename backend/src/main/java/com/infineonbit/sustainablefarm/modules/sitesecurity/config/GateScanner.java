package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the controller methods (or controllers) that the gate scanner may call
 * with SITESECURITY_GATE_TOKEN. Every other endpoint refuses that token.
 * No endpoint uses it yet: the scan endpoint comes with the access loop.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface GateScanner {
}
