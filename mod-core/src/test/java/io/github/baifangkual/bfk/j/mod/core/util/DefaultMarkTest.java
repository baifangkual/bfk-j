package io.github.baifangkual.bfk.j.mod.core.util;

import io.github.baifangkual.bfk.j.mod.core.mark.Default;
import io.github.baifangkual.bfk.j.mod.core.ref.TypeRef;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * @author baifangkual
 * @since 2025/5/25
 */
@SuppressWarnings("CommentedOutCode")
public class DefaultMarkTest {


    @Test
    public void test() {
        Idg idg = Default.get(Idg.class);
        long l = idg.nextId();
        //Map<String, String> stringStringList = Default.get(new TypeRef<>() {});
        //List<Integer> ins = Default.get(new TypeRef<>() {
        //});
        //System.out.println("gen Id: " + l);
        Assertions.assertTrue(l > 0);
    }
}
