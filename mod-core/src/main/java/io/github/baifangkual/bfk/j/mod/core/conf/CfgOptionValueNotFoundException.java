package io.github.baifangkual.bfk.j.mod.core.conf;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;

import java.io.Serial;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 表示在配置类实例中找不到配置项配置值的异常
 *
 * @author baifangkual
 * @since 2024/6/15 v0.0.3
 */
public class CfgOptionValueNotFoundException extends NoSuchElementException {

    @Serial
    private static final long serialVersionUID = 1919810L;

    CfgOptionValueNotFoundException(String errMsg) {
        super(errMsg);
    }

    CfgOptionValueNotFoundException(Cfg.Option<?> option) {
        super(buildingNotFoundValueMsg(option, true));
    }


    /**
     * 表示找不到配置值的异常模板信息
     */
    private static final String NO_VALUE_ERR_MSG_TEMPLATE = "Not found value for Cfg.Option({})";
    /**
     * 追加的配置项默认值信息
     */
    private static final String APPEND_OPTION_DEFAULT_VALUE = "\nCfg.Option.DefaultValue:\n\t{}";
    /**
     * 追加的配置项的说明提示
     */
    private static final String APPEND_OPTION_DESCRIPTION = "\nCfg.Option.Description:\n\t{}";
    /**
     * 追加的配置项找不到的信息
     */
    private static final String APPEND_OPTION_VALUE_NOT_FOUND = "\nCfg.Option.NotFoundValueMsg:\n\t{}";
    /**
     * 追加的failBackOf的信息
     */
    private static final String APPEND_OPTION_FAIL_BACK = "\nCfg.Option.FallbackOf:\n\t{}";

    /**
     * 构造找不到配置值的异常信息过程
     *
     * @param option               配置项
     * @param appendFallbackOptMsg 表示是否需要追加fallbackOption的信息
     * @return msg for not found
     */
    @SuppressWarnings("StringConcatenationInLoop")
    static String buildingNotFoundValueMsg(Cfg.Option<?> option, boolean appendFallbackOptMsg) {
        final String key = option.key();
        final String nullableDescription = option.description()
                .filter(desc -> !desc.isBlank())
                .orElse(null);
        final String nullableNoValueMsg = option.notFoundValueMsg()
                .filter(nvm -> !nvm.isBlank())
                .orElse(null);
        final Object nullableDefValue = option.defaultValue();

        String msg = STF.f(NO_VALUE_ERR_MSG_TEMPLATE, key);
        msg = nullableDefValue == null ? msg : msg + STF.f(APPEND_OPTION_DEFAULT_VALUE, nullableDefValue);
        msg = nullableDescription == null ? msg : msg + STF.f(APPEND_OPTION_DESCRIPTION, nullableDescription);
        msg = nullableNoValueMsg == null ? msg : msg + STF.f(APPEND_OPTION_VALUE_NOT_FOUND, nullableNoValueMsg);

        if (!appendFallbackOptMsg) {
            return msg;
        }

        List<? extends Cfg.Option<?>> fallbackOfOpt = Optional.ofNullable(option.fallbackOf())
                .filter(fbl -> !fbl.isEmpty())
                .orElse(null);
        // 为空则直接返回，否则追加fallback的信息
        if (fallbackOfOpt == null) {
            return msg;
        }
        List<String> fallbackOfOptKeys = fallbackOfOpt.stream().map(Cfg.Option::key).toList();
        msg = msg + STF.f(APPEND_OPTION_FAIL_BACK, fallbackOfOptKeys);


        for (Cfg.Option<?> fbOpt : fallbackOfOpt) {
            msg = msg + "\n\n" + buildingNotFoundValueMsg(fbOpt, false);
        }

        return msg;
    }
}
