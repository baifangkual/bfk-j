package io.github.baifangkual.bfk.j.mod.core.mark;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author baifangkual
 * @since 2025/5/24 v0.0.7
 */
public interface Default {

    // todo SPI IMPL
    @Retention(RetentionPolicy.RUNTIME)
    @interface prov {
        String method();
    }
}
