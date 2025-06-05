package io.github.baifangkual.jlib.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author baifangkual
 * @since 2025/6/5
 */
public class StfTest {

    @Test
    public void test() {

        String str = Stf.f("a,b,{},d,e,\\{},\\\\g", "c");
        Assertions.assertEquals("a,b,c,d,e,\\{},\\\\g", str);
    }
}
