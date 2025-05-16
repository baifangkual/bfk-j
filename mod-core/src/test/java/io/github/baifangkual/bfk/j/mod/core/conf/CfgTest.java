package io.github.baifangkual.bfk.j.mod.core.conf;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author baifangkual
 * @since 2025/5/5
 */
public class CfgTest {


    @Test
    public void test01() {

        Cfg cfg = Cfg.newCfg();

        Cfg.Option<Boolean> opt1 = Cfg.Option.of("opt1")
                .booleanType()
                .defaultValue(true)
                .build();

        cfg.set(opt1, true);
        Assertions.assertEquals(true, cfg.get(opt1));

    }

    @Test
    public void test02() {
        // not found msg test
        Cfg cfg = Cfg.newCfg();

        Cfg.Option<String> opt = Cfg.Option.of("opt1.req")
                .stringType()
                .defaultValue("defaultString")
                .description("this is opt1.req description")
                .notFoundValueMsg("this is opt1.req noValueMsg")
                .build();

        Optional<String> s = cfg.tryGet(opt);
        Assertions.assertEquals(Optional.empty(), s);

    }

    @SuppressWarnings("CommentedOutCode")
    @Test
    public void test03() {
        // not found msg test
        Cfg cfg = Cfg.newCfg();

        Cfg.Option<String> opt1FallBack1 = Cfg.Option.of("opt1.fallback1")
                .stringType()
                .defaultValue("opt1FallBack1DefaultValue")
                .notFoundValueMsg("this is opt1.fallback1 noValueMsg")
                .build();

        Cfg.Option<String> opt1FallBack2 = Cfg.Option.of("opt1.fallback2")
                .stringType()
                .description("this is opt1.fallback2 description")
                .notFoundValueMsg("this is opt1.fallback2 noValueMsg")
                .build();

        Cfg.Option<String> opt1FallBack3 = Cfg.Option.of("opt1.fallback3")
                .stringType()
                .defaultValue("opt1FallBack3DefaultValue")
                .description("this is opt1.fallback3 description")
                .build();

        Cfg.Option<String> opt = Cfg.Option.of("opt1.req")
                .stringType()
                .defaultValue("defaultString")
                .description("this is opt1.req description")
                .notFoundValueMsg("this is opt1.req noValueMsg")
                .fallbackOf(opt1FallBack1, opt1FallBack2, opt1FallBack3)
                .build();
        Assertions.assertThrows(Cfg.OptionValueNotFoundException.class, () -> {
            String s = cfg.get(opt);
            System.out.println(s);
        });
    }

    @Test
    public void test04() {
        // get unsafeGet and getOrDefault and unsafeGetOrDefault test
        Cfg cfg = Cfg.newCfg();

        final String defaultString = "defaultString";

        Cfg.Option<String> strOpt = Cfg.Option.of("cfgOption")
                .defaultValue(defaultString)
                .description("this is opt.cfgOption description")
                .build();

        Optional<String> s = cfg.tryGet(strOpt);
        Assertions.assertEquals(Optional.empty(), s);
        Assertions.assertThrows(Cfg.OptionValueNotFoundException.class, () -> cfg.get(strOpt));
        Optional<String> orDefault = cfg.tryGetOrDefault(strOpt);
        Assertions.assertEquals(Optional.of(defaultString), orDefault);
        Assertions.assertEquals(defaultString, cfg.getOrDefault(strOpt));


    }

    @Test
    public void test05() {
        Cfg cfg = Cfg.newCfg();
        final String defaultString = "defaultString";
        Cfg.Option<String> strOpt1 = Cfg.Option.of("cfgOption")
                .stringType()
                .build();

        Cfg.Option<String> strOpt2 = Cfg.Option.of("cfgOption")
                .stringType()
                .build();

        cfg.set(strOpt1, defaultString);
        Assertions.assertThrows(Cfg.OptionKeyDuplicateException.class, () -> cfg.set(strOpt2, defaultString));

    }

    @Test
    public void test06() {
        // reset test
        Cfg cfg = Cfg.newCfg();
        final String defaultString = "defaultString";
        Cfg.Option<String> strOpt1 = Cfg.Option.of("cfgOption")
                .stringType()
                .build();

        Cfg.Option<String> strOpt2 = Cfg.Option.of("cfgOption")
                .stringType()
                .build();

        cfg.set(strOpt1, defaultString);
        cfg.reset(strOpt2, defaultString);
    }

    @Test
    public void test07() {
        Cfg cfg = Cfg.newCfg();
        final String defaultString = "defaultString";
        Cfg.Option<String> strOpt1 = Cfg.Option.of("cfgOption")
                .stringType()
                .build();

        cfg.set(strOpt1, defaultString);
        final Cfg ocp = Cfg.ofMap(new HashMap<>(cfg.toReadonlyMap()));
        Assertions.assertEquals(ocp, cfg);
    }
}
