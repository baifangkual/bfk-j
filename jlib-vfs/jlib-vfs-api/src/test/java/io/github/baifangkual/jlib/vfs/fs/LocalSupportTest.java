package io.github.baifangkual.jlib.vfs.fs;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author baifangkual
 * @since 2025/6/10
 */
public class LocalSupportTest {


    @Test
    public void test2() {
        LocalSupport.LocalType localType = LocalSupport.localType;
        List<String> roots = LocalSupport.newCurrCleanedRootDirNames();
        switch (localType) {
            case linuxLike -> Assertions.assertEquals(1, roots.size());
            case windows -> Assertions.assertNotEquals("/", roots.stream().findAny().orElseThrow());
        }
//        System.out.println(localType);
//        for (String s : roots) {
//            System.out.println(s);
//        }
    }


}
