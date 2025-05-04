package io.github.baifangkual.bfk.j.mod.core.conf;

import io.github.baifangkual.bfk.j.mod.core.fmt.STF;
import io.github.baifangkual.bfk.j.mod.core.model.Tup2;
import io.github.baifangkual.bfk.j.mod.core.panic.Err;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Supplier;

/**
 * 配置类对象，状态可变，本身线程不安全，若需要将该对象用以线程共享变量，则应当使用 {@link #toImmutable()} 方法共享不可变只读Config对象<br>
 * 该对象参考seatunnel.Config、ReadOnlyConfig和flink.Config创建<br>
 * 该对象被标记为可序列化的，其实将可序列化条件委托至内部Map[str, obj]的可序列化，即要求Map中value为可序列化的<br>
 * 该对象设定为运行时对象，非传输对象，遂对Jackson等JSON序列化和反序列化支持并不友好，如果一定要通过JSON序列化和反序列化该类型，
 * 可选择向Jackson实现{link com.fasterxml.jackson.databind.JsonSerializer}和{link com.fasterxml.jackson.databind.JsonDeserializer},
 * 或选择使用序列化{@link #immutableViewMap()}及反序列化{@link #ofMap(Map)} 结果，因为该对象直接保存的键值类型信息存储在{@link Option}中，
 * 而非Config本身中，所以通过JSON反序列化的Config可能在Get键值时出现类型转换异常{@link ClassCastException},需注意该点<br>
 * Config的key要求类型为{@link Option},该类型携带的泛型参数表示该配置条目（key=value）的value的类型<br>
 * 20241206：经测试，已知json序列化因为类型丢失，且java的泛型擦除，遂该类型在通过json序列化后反序列化时可能导致引用的类型异常，
 * 遂对该类型使用序列化，最好使用java原生的序列化方式<br>
 * 使用：
 * <pre>
 *     {@code
 *     // 定义key
 *     Config.Option<Boolean> IS_TRUE = Config.Option.ofRequired("is_true", false);
 *     Config.Option<List<String>> NAMES = Config.Option.ofNotRequired("names", List.of("a", "b", "c"));
 *     // 创建
 *     Config config = Config.of();
 *     // 设置值
 *     config.set(IS_TRUE, true).set(...);
 *     config.reset(NAMES, List.of("d", "e", "f"));
 *     // 获取值
 *     Optional<Boolean> isTrueOption = config.get(IS_TRUE);
 *     Boolean getOrDef = config.getOrDefault(IS_TRUE);
 *     Boolean unsafeGetBool = config.unsafeGet(IS_TRUE);
 *     List<String> unsafeGetNames = config.unsafeGet(NAMES);
 *     }
 * </pre>
 *
 * @author baifangkual
 * @see Option
 * @see Option.TypeRef
 * @see Option.KeyBindBuilder
 * @see Option.Builder
 * @since 2024/6/18
 */
public class Conf implements Iterable<Tup2<String, Object>>, Serializable {

    // todo 20250504 详细修改该

    @Serial
    private final static long serialVersionUID = -1L;

    private final Map<String, Object> map;

    public Conf(@NonNull Supplier<? extends Map<String, Object>> configMapSup) {
        map = configMapSup.get();
    }

    public Conf() {
        this(HashMap::new);
    }

    public static Conf of() {
        return new Conf();
    }

    public static Conf ofMap(@NonNull Map<? extends String, ?> map) {
        return of().setFromMap(map);
    }

    /**
     * 返回该配置类的不可修改视图，因为是视图，所以返回的视图本身不可修改，但原对象map引用（即原Config）本身是可修改的
     *
     * @return Map[String, Obj]
     */
    public Map<String, Object> immutableViewMap() {
        return Collections.unmodifiableMap(map);
    }

    private static final Conf EMPTY = new Conf(Collections::emptyMap);

    /**
     * 返回表示当前配置类对象是否为空的布尔值，这里“空”是表示：当前配置类对象中没有任何一个配置项
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 与其他集合行为类似，返回一个空的不可变的该类型引用
     *
     * @return 空配置
     */
    public static Conf readonlyEmpty() {
        return EMPTY;
    }

    /**
     * 返回一个不可修改的Config对象，并非将当前Config转换为不可修改，原对象map引用（即原Config）本身是可修改的
     *
     * @return immutable Config
     */
    public Conf toImmutable() {
        return new Conf(this::immutableViewMap);
    }

    /**
     * 向Config对象中设定某键值配置对，若某键在该Config对象中已存在（即已经设定）该方法将会抛出异常<br>
     * 若需安全的无视重新设置约束，可显示调用{@link #reset(Option, Object)} 而非该方法
     *
     * @param option {@link Option} 配置键
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return Config，this
     */
    public <T> Conf set(@NonNull Conf.Option<T> option, @NonNull T value) {
        String key = option.getKey();
        Err.realIf(this.map.containsKey(key),
                IllegalArgumentException::new, "Option key:[{}] already exists", key);
        this.map.put(key, value);
        return this;
    }

    /**
     * @param condition 条件，当条件为true，则向Config设置键值对
     * @param option    {@link Option} 配置键
     * @param value     配置值
     * @param <T>       该键值对存储的配置值的类型
     * @return Config，this
     * @see #set(Option, Object)
     */
    public <T> Conf setIf(boolean condition, Option<T> option, T value) {
        if (condition) {
            set(option, value);
        }
        return this;
    }

    /**
     * 当value不为null，设置键值
     *
     * @param option {@link Option} 配置键
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return Config，this
     * @see #setIf(boolean, Option, Object)
     */
    public <T> Conf setIfNotNull(Option<T> option, T value) {
        setIf(value != null, option, value);
        return this;
    }

    /**
     * 向Config对象中设定某键值对，相对于{@link #set(Option, Object)}方法，即使某键在对象中已经被设定，该方法也不会抛出异常
     *
     * @param option {@link Option} 配置键
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return Config，this
     */
    public <T> Conf reset(@NonNull Conf.Option<T> option, @NonNull T value) {
        String key = option.getKey();
        map.put(key, value);
        return this;
    }

    /**
     * @param condition 条件，当条件为true，则向Config设置键值对
     * @param option    {@link Option} 配置键
     * @param value     配置值
     * @param <T>       该键值对存储的配置值的类型
     * @return Config，this
     * @see #reset(Option, Object)
     */
    public <T> Conf resetIf(boolean condition, Option<T> option, T value) {
        if (condition) {
            reset(option, value);
        }
        return this;
    }

    /**
     * 当value不为null，设置值
     *
     * @param option {@link Option} 配置键
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return Config，this
     * @see #resetIf(boolean, Option, Object)
     */
    public <T> Conf resetIfNotNull(Option<T> option, T value) {
        resetIf(value != null, option, value);
        return this;
    }

    /**
     * 当条件成立，从配置config中删除指定配置值和键，
     * 可能config中并未有指定的键值对，但该方法的删除目的为“确保删除逻辑的最终一致性”，
     * 即传入指定的配置键，该方法调用的最终结果能够确保配置中无指定键值
     *
     * @param condition 条件表达式
     * @param option    config键
     * @return config this
     */
    public <T> Conf removeIf(boolean condition, Option<T> option) {
        if (condition) {
            map.remove(option.getKey());
        }
        return this;
    }

    /**
     * 从配置config中删除指定配置值和键
     *
     * @param option config键
     * @return config this
     * @see #removeIf(boolean, Option)
     */
    public <T> Conf remove(Option<T> option) {
        return removeIf(true, option);
    }

    /**
     * 向Config中批量设定某Map提供的值，若Config中已有某键，则该方法抛出异常
     *
     * @param map Map[?:str, ?]
     * @return Config, this
     * @throws IllegalArgumentException 当当前配置类中已有给定的map中的键时
     */
    private Conf setFromMap(Map<? extends String, ?> map) {
        // tod 当key相同，这里会覆盖掉原有的值，遂这里应当进行判断
        map.forEach((othKey, othValue) -> {
            Err.realIf(this.map.containsKey(othKey),
                    IllegalArgumentException::new, "Option key: [{}] already exists", othKey);
            this.map.put(othKey, othValue);
        });
        return this;
    }

    /**
     * 向Config中批量设置某Map提供的值，与{@link #setFromMap(Map)}不同，当Config
     * 中已有map中的键时，不会抛出异常
     *
     * @param map Map[?:str, ?]
     * @return Config, this
     */
    private Conf resetFromMap(Map<? extends String, ?> map) {
        this.map.putAll(map);
        return this;
    }

    /**
     * 给定配置键{@link Option} 返回Config中指定配置键所对应的值，
     * 当Config中未有该配置键时，则以配置的{@link Option#fallbackOf}顺序找配置值，
     * 若在该情况下仍未找到配置值，则尝试使用给定的配置键{@link Option}的默认值，若给定的{@link Option}的默认值
     * 为null，则开始以配置的{@link Option#fallbackOf}顺序使用定义的默认值，在仍未找到任意可用的值的情况下，
     * 该方法将返回null，遂该方法不能保证空安全，若需要保证空安全且使用同样的逻辑寻找配置,
     * 应使用{@link #tryGetOrDefault(Option)}方法<br>
     * 显式调用此方法应当确保配置键有默认值，否则则可能返回空
     *
     * @param option {@link Option} 配置键
     * @param <T>    配置值类型
     * @return 配置值 nullable value
     */
    public <T> T getOrDefault(@NonNull Conf.Option<T> option) {
        /*
        20241015：
        经过一段时间使用，发现该方法的一个问题：
        假设Config.Option配置了defaultValue 和 failBack
        原有的该方法逻辑为：先从Config中找value，当没有value时，使用defaultValue，当没有
        defaultValue时则使用failBack的key寻找，当failBack的key未找到value时，则使用failBack的
        defaultValue...,failBack以此类推....
        这种情况可能不符合该方法提供的逻辑的第一感觉，并且在使用一段时间后发现这种逻辑有一定问题：
        假设某Config.Option配置了key为 "1"，defaultValue值为 "defValue1"，
        且其第一个failBack的key为 "2" ...
        当Config中有配置key为 "2" 且value为 "noDefValue2" 时，使用该Config.Option时，
        将会通过该方法获得值 "defValue1"，而其实通过它的failBack能够从Config中找到一个实际配置的值，
        遂在这种情况下，该方法的表意可以说有一定歧义，考虑到该方法已经在部分模块中或其他人的代码中已经使用，
        遂不对方法API做变更，仅对方法逻辑和方法API DOC进行变更，该方法新的逻辑为：
        先通过Config.Option和其的failBack从Config中找存在的配置值，当找到配置值时，返回配置值
        当未找到配置值时，则开始以同样的顺序从Config.Option和其的failBack中找defaultValue，
        若找到defaultValue后，返回找到的第一个可用的非空的defaultValue，否则，该方法返回null
         */
        T nullableValue = get(option).orElse(null);
        if (nullableValue == null) {
            // get 并且也使用了failBack 还是为null，则开始寻找并使用默认值
            nullableValue = option.getDefaultValue();
            if (nullableValue == null) {
                // 给定的默认值仍为null，则开始寻找使用failBack的默认值
                List<Option<T>> fallbackOf = option.fallbackOf;
                if (fallbackOf != null && !fallbackOf.isEmpty()) {
                    for (Option<T> fbOption : fallbackOf) {
                        T fbDefaultValue = fbOption.getDefaultValue();
                        if (fbDefaultValue != null) {
                            // fb的默认值也按照list顺序寻找，若找到，则直接返回顺序第一个非空的，
                            // 否则，循环到最后仍没有找到，则返回null
                            nullableValue = fbDefaultValue;
                            break;
                        }
                    }
                }
            }
        }
        return nullableValue;
    }

    public <T> Optional<T> tryGetOrDefault(@NonNull Conf.Option<T> option) {
        return Optional.ofNullable(getOrDefault(option));
    }

    /**
     * 给定配置键{@link Option},返回指定的配置键所对应的值，当Config中未有该配置键时，则返回给定的defaultValue值
     *
     * @param option     配置键
     * @param otherValue 当给定配置键所对应的值不存在时，使用该值作为返回值
     * @param <T>        配置值类型
     * @return 配置值
     */
    public <T> T getOr(@NonNull Conf.Option<T> option, T otherValue) {
        return get(option).orElse(otherValue);
    }

    public <T> T getOr(@NonNull Conf.Option<T> option, Supplier<? extends T> otherValueSupplier) {
        return get(option).orElseGet(otherValueSupplier);
    }

    /**
     * 给定配置键{@link Option} 返回Config中指定配置键所对应的值，
     * 当Config中未有该配置键时，则返回配置键中给定的默认值，若默认值为null，则抛出异常<br>
     * 这个方法是{@link #getOrDefault(Option)} 的严格版本
     *
     * @param option {@link Option} 配置键
     * @param <T>    配置值类型
     * @return 配置值
     * @throws NoSuchElementException 当给定配置键不在Config对象中记录并且配置键无默认值时抛出
     */
    public <T> T unsafeGet(@NonNull Conf.Option<T> option) throws NoSuchElementException {
        return Optional.ofNullable(getOrDefault(option))
                .orElseThrow(() -> new ConfigValueNotFoundException(buildingNoValueMsg(option)));
    }

    /**
     * 表示找不到配置值的异常模板信息
     */
    private static final String NO_VALUE_ERR_MSG_TEMPLATE = "No value found for Config.Option \"{}\"";
    private static final String APPEND_OPTION_DESCRIPTION = "\nConfig.Option.Description:\n\t{}";

    /**
     * 构造找不到配置值的异常信息过程
     *
     * @param option 配置项
     * @return msg for not found
     */
    private static String buildingNoValueMsg(Option<?> option) {
        String msg = STF.f(NO_VALUE_ERR_MSG_TEMPLATE, option.getKey());
        String nullableOptionDescription = Optional.ofNullable(option.getDescription())
                .filter(desc -> !desc.isBlank())
                .orElse(null);
        msg = nullableOptionDescription == null ? msg : msg + STF.f(APPEND_OPTION_DESCRIPTION, nullableOptionDescription);
        return option.getNoValueMsg() == null ? msg : msg + ", " + option.getNoValueMsg();
    }


    /**
     * 表示找不到配置值的异常
     */
    public static class ConfigValueNotFoundException extends NoSuchElementException {
        public ConfigValueNotFoundException(String errMsg) {
            super(errMsg);
        }
    }

    /**
     * 给定配置键{@link Option} 返回Config中指定配置键所对应的值的{@link Optional}包装，
     * 当Config中未有该配置键时，则返回的实际值为{@link Optional#empty()}<br>
     *
     * @param option {@link Option} 配置键
     * @param <T>    配置值类型
     * @return 配置值
     */
    public <T> Optional<T> get(@NonNull Conf.Option<T> option) {
        String key = option.getKey();
        T nullableValue = preciseGetByKey(key);
        if (nullableValue == null) {
            List<Option<T>> fallbackOf = option.fallbackOf;
            if (fallbackOf != null && !fallbackOf.isEmpty()) {
                for (Option<T> fallbackOpt : fallbackOf) {
                    T v = preciseGetByKey(fallbackOpt.getKey());
                    if (v != null) {
                        nullableValue = v;
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(nullableValue);
    }

    @SuppressWarnings("unchecked")
    private <T> T preciseGetByKey(@NonNull String key) {
        Object nullableValue = map.get(key);
        return (T) nullableValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conf conf)) return false;
        return map.equals(conf.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[" + map + "]";
    }

    @Override
    public Iterator<Tup2<String, Object>> iterator() {
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Tup2<String, Object> next() {
                Map.Entry<String, Object> next = it.next();
                return Tup2.of(next.getKey(), next.getValue());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    /**
     * {@link Conf}对象的配置选项表示，该对象不可变，线程安全，可共享，建议定义至静态字段<br>
     * 使用：
     * <pre>
     *     {@code Config.Option<Table> TABLE = Config.Option.of("table")
     *             .type(Table.class)
     *             .required(true)
     *             .defaultValue(new Table().setName("test_table"))
     *             .description("oh")
     *             .build();}
     *             Config.key定义（完整），通过{@link KeyBindBuilder#type(Class)} 方法表示泛型类型
     *     {@code Config.Option<List<Table>> TABLES = ...of("tables")
     *             .typeRef(new Config.Option.TypeRef<List<Table>>() {})
     *             ....build();}
     *             因为java泛型的残缺，Class无法表达参数化类型，遂使用{@link TypeRef}表示参数化类型
     *     {@code Config.Option<String> USER = Config.Option.of("user")
     *             .stringType()
     *             ....build();}
     *             Config.key定义（简单类型）,Str、int、bool、float等等基本类型，有现成方法
     *     {@code Config.Option<Boolean> IS_MAN = Config.Option.ofNotRequired("is_man");}
     *             或通过更简单的方式定义key
     * </pre>
     *
     * @param <T> {@link Conf} 中该{@link Option} 所对应的Value的类型
     */
    @Getter
    @ToString
    public static class Option<T> {
        /**
         * 配置项标识-同一个{@link Conf}中不同的{@link Option}配置项标识必须不同
         */
        private final String key;
        /**
         * 配置项默认值-当{@link Conf}中找不到该配置项的值时，将使用该默认值，该默认值优先级低于{@link Option#fallbackOf}配置项
         */
        private final T defaultValue;
        /**
         * 表达该配置项是否为必要配置项
         */
        private final boolean required;
        /**
         * 配置项描述
         */
        private final String description;
        /**
         * 当找不到该配置值时的信息
         */
        private final String noValueMsg;
        /**
         * 当找不到该配置项时，将使用的“回滚”配置项
         */
        private final List<Option<T>> fallbackOf;

        public Option(String key, T defaultValue, boolean required) {
            this(key, defaultValue, required, null);
        }

        public Option(String key, T defaultValue, boolean required, String description) {
            this(key, defaultValue, required, description, null);
        }

        public Option(String key, T defaultValue, boolean required, String description, String noValueMsg) {
            this(key, defaultValue, required, description, noValueMsg, Collections.emptyList());
        }

        public Option(String key, T defaultValue, boolean required, String description, String noValueMsg, List<Option<T>> fallbackOf) {
            if (key == null) {
                throw new IllegalArgumentException("key must not be null");
            }
            this.key = key;
            this.defaultValue = defaultValue;
            this.required = required;
            this.description = description;
            this.noValueMsg = noValueMsg;
            this.fallbackOf = fallbackOf == null ? Collections.emptyList() : fallbackOf;
        }

        public static <V> Option<V> ofRequired(String key) {
            return new Option<>(key, null, true);
        }

        public static <V> Option<V> ofNotRequired(String key) {
            return new Option<>(key, null, false);
        }

        public static <V> Option<V> ofRequired(String key, V defaultValue) {
            return new Option<>(key, defaultValue, true);
        }

        public static <V> Option<V> ofNotRequired(String key, V defaultValue) {
            return new Option<>(key, defaultValue, false);
        }

        public static <V> Option<V> ofRequired(String key, V defaultValue, String description) {
            return new Option<>(key, defaultValue, true, description);
        }

        public static <V> Option<V> ofNotRequired(String key, V defaultValue, String description) {
            return new Option<>(key, defaultValue, false, description);
        }

        /**
         * 因为java泛型的残缺，Class无法表示参数化类型，遂参考jackson等，有此类，表示携带泛型参数的类型<br>
         * 示例，表示Map[str, Table]类型而非Map类型:<br>
         * {@code TypeRef<Map<String, Table>> tableMapType = new TypeRef<>(){}}
         *
         * @param <T> 表示携带泛型的类型
         */
        @Getter
        public static abstract class TypeRef<T> {
            private final Type rawType;

            protected TypeRef() {
                this.rawType = getSuperclassTypeParameter(getClass());
            }

            private Type getSuperclassTypeParameter(Class<?> clazz) {
                Type genericSuperclass = clazz.getGenericSuperclass();
                if (genericSuperclass instanceof Class) {
                    // try to climb up the hierarchy until meet something useful
                    if (TypeRef.class != genericSuperclass) {
                        return getSuperclassTypeParameter(clazz.getSuperclass());
                    }
                    throw new IllegalArgumentException("'" + getClass() + "' extends TypeRef but misses the type parameter. "
                                                       + "Remove the extension or add a type parameter to it.");
                }
                Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
                if (rawType instanceof ParameterizedType) {
                    rawType = ((ParameterizedType) rawType).getRawType();
                }

                return rawType;
            }

            @Override
            public String toString() {
                return "TypeRef:<" + rawType.toString() + ">";
            }
        }

        public static KeyBindBuilder of(String key) {
            return new KeyBindBuilder(key);
        }

        /**
         * Config.Option builder，key绑定，作为Config.Option构建过程中的中间对象
         */
        @RequiredArgsConstructor
        public static class KeyBindBuilder {
            private final String key;

            public <T> Builder<T> defaultValue(T defaultValue) {
                Err.realIf(defaultValue == null, "Default value is null");
                return new Builder<T>(key).defaultValue(defaultValue);
            }

            public <T> Builder<T> type(Class<T> type) {
                return new Builder<T>(key);
            }

            public <T> Builder<T> type() {
                return new Builder<T>(key);
            }

            public <T> Builder<T> typeRef(TypeRef<T> type) {
                return new Builder<T>(key);
            }

            public Builder<String> stringType() {
                return new Builder<>(key);
            }

            public Builder<Integer> intType() {
                return new Builder<>(key);
            }

            public Builder<Long> longType() {
                return new Builder<>(key);
            }

            public Builder<Float> floatType() {
                return new Builder<>(key);
            }

            public Builder<Double> doubleType() {
                return new Builder<>(key);
            }

            public Builder<Boolean> booleanType() {
                return new Builder<>(key);
            }

        }

        /**
         * Config.Option builder 作为Config.Option对象构建过程的中间对象，生命周期极短
         *
         * @param <T> Config.Option表示的类型，即该OptionKey对应的value的类型
         */
        public static class Builder<T> {

            private final String key;

            private T defaultValue;

            private boolean required;

            private String description;

            private String noValueMsg;

            private List<Option<T>> fallbackOf;

            private Builder(String key) {
                this.key = key;
            }

            public Builder<T> defaultValue(T defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            public Builder<T> required(boolean required) {
                this.required = required;
                return this;
            }

            public Builder<T> description(String description) {
                this.description = description;
                return this;
            }

            public Builder<T> noValueMsg(String noValueMsg) {
                this.noValueMsg = noValueMsg;
                return this;
            }

            public Builder<T> fallbackOf(Option<T> fallbackOf) {
                if (this.fallbackOf == null) {
                    this.fallbackOf = new ArrayList<>();
                }
                this.fallbackOf.add(fallbackOf);
                return this;
            }

            public Builder<T> fallbackOf(List<Option<T>> fallbackOf) {
                if (this.fallbackOf == null) {
                    this.fallbackOf = new ArrayList<>(fallbackOf); // 可读可写
                } else {
                    this.fallbackOf.addAll(fallbackOf);
                }
                return this;
            }

            public Option<T> build() {
                return new Option<>(key, defaultValue, required, description, noValueMsg, fallbackOf);
            }
        }


    }
}
