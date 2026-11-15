package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** On a controller: every endpoint requires the admin role, reads included. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminOnly {
}
