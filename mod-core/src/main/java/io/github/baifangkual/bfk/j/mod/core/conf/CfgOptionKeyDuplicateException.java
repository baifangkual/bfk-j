package io.github.baifangkual.bfk.j.mod.core.conf;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;

import java.io.Serial;

/**
 * 表示在一个配置类实例中设置{@link Cfg.Option#key()}重复的配置项
 *
 * @author baifangkual
 * @since 2025/5/10
 */
class CfgOptionKeyDuplicateException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1919810L;

    CfgOptionKeyDuplicateException(String optionKey) {
        super(buildErrMsgByOptionKey(optionKey));
    }

    CfgOptionKeyDuplicateException(Cfg.Option<?> option) {
        super(buildErrMsgByOption(option));
    }

    /**
     * 重复配置的异常信息模板
     */
    private static final String EXISTS_MSG_TEMP = "Cfg.Option({}) already exists in Cfg instance";


    static String buildErrMsgByOption(Cfg.Option<?> option) {
        return buildErrMsgByOptionKey(option.key());
    }

    static String buildErrMsgByOptionKey(String optionKey) {
        return STF.f(EXISTS_MSG_TEMP, optionKey);
    }
}
