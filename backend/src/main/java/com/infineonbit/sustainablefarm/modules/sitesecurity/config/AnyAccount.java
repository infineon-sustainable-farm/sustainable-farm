package com.infineonbit.sustainablefarm.modules.sitesecurity.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any operator with an active Site Security role may call this endpoint, for
 * example to read its own authorization. Endpoints without this annotation or
 * {@link UserPermission} are admin-only: users are denied by default.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AnyAccount {
}
