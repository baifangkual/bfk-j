package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.ref.TypeRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * @since 2025/5/8
 */
public class TypeRefTest {


    @Test
    public void test01() {
        TypeRef<List<String>> lsr = new TypeRef<List<String>>() {
        };
        Type type = lsr.type();
        Assertions.assertEquals("java.util.List<java.lang.String>", type.toString());
    }

    @Test
    public void test02() {
        TypeRef<List<String>> lsr = new TypeRef<List<String>>() {
        };
        Assertions.assertEquals("TypeRef<java.util.List<java.lang.String>>", lsr.toString());
    }

    @Test
    public void test03() {
        TypeRef<List<String>> lsr = new TypeRef<>() {};
        Assertions.assertEquals("java.util.List<java.lang.String>", lsr.type().toString());
        Assertions.assertEquals("TypeRef<java.util.List<java.lang.String>>", lsr.toString());
        TypeRef<Map<String, Integer>> msi = new TypeRef<>() {};
        Assertions.assertEquals("java.util.Map<java.lang.String, java.lang.Integer>", msi.type().toString());
        Assertions.assertEquals("TypeRef<java.util.Map<java.lang.String, java.lang.Integer>>", msi.toString());
    }
}
