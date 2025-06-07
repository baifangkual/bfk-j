package io.github.baifangkual.jlib.core.conf;

import io.github.baifangkual.jlib.core.lang.Tup2;
import io.github.baifangkual.jlib.core.mark.Iter;
import io.github.baifangkual.jlib.core.panic.Err;
import io.github.baifangkual.jlib.core.ref.TypeRef;
import io.github.baifangkual.jlib.core.util.Stf;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;

/**
 * <b>配置类</b><br>
 * <p>存储配置信息，可存储0到n个配置信息，一个配置信息以一个“配置项”的形式表示，一个“配置项”包含一个“配置键”和一个“配置值”，
 * 与配置类通过{@link #set(Option, Object)}、{@link #tryGet(Option)}等方式交互的{@link Option}携带的泛型参数描述了一个配置值的类型<br>
 * 一个配置类实体中，不会存在两个或以上的配置项的{@link Option#key()}(“配置键”)相同，
 * 若通过{@link #set(Option, Object)}设置的两个配置项的“配置键”相同，则在设置第二个配置项时将抛出异常{@link OptionKeyDuplicateException}，
 * 而通过{@link #reset(Option, Object)}设置两个配置项的“配置键”相同的配置时，后设置的配置项将覆盖先设置的配置项
 * <p>通过{@link #tryGet(Option)}或{@link #get(Option)}获取配置值时，将不会使用配置项的默认值({@link Option#defaultValue()})，
 * 而通过{@link #tryGetOrDefault(Option)}或{@link #getOrDefault(Option)}获取配置值时，若配置类中没有该配置项，则将使用配置项的默认值<br>
 * 该配置类实体状态可变，线程不安全，若需要将该对象用以线程共享变量，则应当使用 {@link #toReadonly()} 方法共享不可变只读Cfg对象<br>
 * 该配置类参考seatunnel.Config、ReadOnlyConfig和flink.Config创建<br>
 * 该配置类实体对象设定为运行时对象，非传输对象，遂对Jackson等JSON序列化和反序列化支持并不友好，如果一定要通过JSON序列化和反序列化该类型，
 * 可选择向Jackson实现{@code com.fasterxml.jackson.databind.JsonSerializer}和{@code com.fasterxml.jackson.databind.JsonDeserializer},
 * 或选择使用序列化{@link #toReadonlyMap()}及反序列化{@link #ofMap(Map)} 结果，因为该对象直接保存的键值类型信息存储在{@link Option}中，
 * 而非Cfg本身中，所以通过JSON反序列化的Cfg可能在Get键值时出现类型转换异常{@link ClassCastException},需注意该点,
 * <p>遂对该类型使用序列化，最好使用java原生的序列化方式，该 impl {@link Serializable}，
 * 但该能否成功序列化取决于内部存储的配置值是否能都序列化
 * <p>如无说明，方法均不允许传入 {@code null}
 * <pre>
 *     {@code
 *     // 定义“配置项”
 *     public static final Cfg.Option<List<Integer>> NUMBERS = Cfg.Option
 *             .of("numbers") // requisite
 *             .type<List<Integer>>() // requisite
 *             .defaultValue(List.of(1, 2, 3))
 *             .description("this is numbers option desc")
 *             .notFoundValueMsg("not found numbers option")
 *             .build();
 *     // 向配置类中设置“配置项” ...
 *     cfg.set(NUMBERS, List.of(3, 2, 1));
 *     // 从配置类中读取“配置值” ...
 *     Optional<List<Integer>> numbersOpt = cfg.tryGet(NUMBERS);
 *     List<Integer> numbers = cfg.get(NUMBERS);
 *     Optional<List<Integer>> numbersOrDefault = cfg.tryGetOrDefault(NUMBERS);
 *     List<Integer> numbersOrDefault = cfg.getOrDefault(NUMBERS);
 *     }
 * </pre>
 *
 * @author baifangkual
 * @see #set(Option, Object)
 * @see #reset(Option, Object)
 * @see #tryGet(Option)
 * @see #get(Option)
 * @see #tryGetOrDefault(Option)
 * @see #getOrDefault(Option)
 * @see Option
 * @see TypeRef
 * @see Option.KeyBindBuilder
 * @see Option.Builder
 * @since 2024/6/18 v0.0.3
 */
public class Cfg implements Iter<Tup2<String, Object>>, Serializable {

    @Serial
    private final static long serialVersionUID = 1919810L;
    /**
     * 存储配置项的实际结构
     */
    private final Map<String, Object> map;

    /**
     * 私有构造，给定一个map的提供者函数，使用该提供者获取存储配置项的map结构
     *
     * @param cfgMapSup map提供者函数
     */
    private Cfg(Supplier<? extends Map<String, Object>> cfgMapSup) {
        map = Optional.ofNullable(cfgMapSup)
                .map(Supplier::get)
                .orElseThrow(() -> new NullPointerException("cfgMapSup is null or map is null"));
    }

    /**
     * 新建一个空的配置类
     *
     * @return 新配置类
     */
    public static Cfg newCfg() {
        return new Cfg(HashMap::new);
    }

    /**
     * 给定一个map，以该map为基础创建配置类，允许该map有值或为empty
     * <p>该方法仅读取给定的map，不会持有该map的引用</p>
     *
     * @param map Map实现类
     * @return 新配置类
     */
    public static Cfg ofMap(Map<? extends String, ?> map) {
        return newCfg().setFromMap(map);
    }

    /**
     * 给定一个map提供者，从该map提供者重获取map并以此为基础创建配置类，允许该map有值或为empty
     *
     * @param fnGetMap Map提供者函数
     * @return 新配置类
     */
    public static Cfg ofMap(Supplier<? extends Map<String, Object>> fnGetMap) {
        return new Cfg(fnGetMap);
    }

    /**
     * 返回该配置类的不可修改视图，返回的视图本身不可修改，但原对象map引用（即原Cfg）本身是可修改的
     *
     * @return 配置类的只读Map视图
     */
    public Map<String, Object> toReadonlyMap() {
        return Collections.unmodifiableMap(map);
    }

    /**
     * 唯一空配置类引用
     */
    private static final Cfg EMPTY = new Cfg(Collections::emptyMap);

    /**
     * 返回表示当前配置类对象是否为空的布尔值，这里“空”是表示：当前配置类对象中没有任何一个配置项
     *
     * @return true 表示该配置类为空，false 表示该配置类不为空
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * 与其他集合或容器对象行为类似，返回一个空的不可变的该类型引用
     *
     * @return 不可变的空配置类
     */
    public static Cfg readonlyEmpty() {
        return EMPTY;
    }

    /**
     * 以当前配置类中的配置项为基础，返回一个不可修改只读的Cfg对象，并非将当前Cfg转换为不可修改，原对象map引用（即原Cfg）本身是可修改的
     *
     * @return immutable readonly Cfg
     */
    public Cfg toReadonly() {
        return new Cfg(this::toReadonlyMap);
    }

    /**
     * 向配置类对象中设定某键值配置对，若某键在该配置类对象中已存在（即已经设定）该方法将会抛出异常<br>
     * 若需安全的无视重新设置约束，可显示调用{@link #reset(Option, Object)} 而非该方法
     *
     * @param option 配置键，不能为null
     * @param value  配置值, 不能为null
     * @param <T>    该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException        当给定的配置键为null或给定的配置值为null时
     * @throws OptionKeyDuplicateException 当给定的配置键已在当前配置类中设定配置时
     */
    public <T> Cfg set(Cfg.Option<T> option, T value) {
        Objects.requireNonNull(option, "Cfg.Option is null");
        Objects.requireNonNull(value, "value is null");
        String key = option.key();
        Err.realIf(this.map.containsKey(key), () -> new OptionKeyDuplicateException(option));
        this.map.put(key, value);
        return this;
    }

    /**
     * @param condition 条件，当条件为true，则向Cfg设置配置项
     * @param option    配置键, 不能为null
     * @param value     配置值，不能为null
     * @param <T>       该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException        当给定的配置键为null或给定的配置值为null时
     * @throws OptionKeyDuplicateException 当给定的配置键已在当前配置类中设定配置时
     * @see #set(Option, Object)
     */
    public <T> Cfg setIf(boolean condition, Option<T> option, T value) {
        if (condition) {
            set(option, value);
        }
        return this;
    }

    /**
     * 若该配置类内找不到指定的配置项，则设置其
     *
     * @param option 配置键
     * @param value  配置值
     * @param <T>    配置值类型
     * @return this
     */
    public <T> Cfg setIfNotSet(Option<T> option, T value) {
        return this.setIf(this.tryGet(option).isEmpty(), option, value);
    }

    /**
     * 当给定的value不为null，设置配置项，与{@link #set(Option, Object)}方法相比，该方法在给定的value为null时不会抛出异常
     *
     * @param option 配置键，不能为null
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException        当给定的配置键为null时
     * @throws OptionKeyDuplicateException 当给定的配置键已在当前配置类中设定配置时
     * @see #setIf(boolean, Option, Object)
     * @deprecated 与 {@link #setIfNotSet(Option, Object)} 有歧义，且用处较小
     */
    @Deprecated(forRemoval = true)
    public <T> Cfg setIfNotNull(Option<T> option, T value) {
        return setIf(value != null, option, value);
    }

    /**
     * 向配置类对象中设定某键值对，相对于{@link #set(Option, Object)}方法，即使某键在对象中已经被设定，该方法也不会抛出异常
     *
     * @param option 配置键, 不能为null
     * @param value  配置值, 不能为null
     * @param <T>    该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException 当给定的配置键为null或给定的配置值为null时
     */
    public <T> Cfg reset(Cfg.Option<T> option, T value) {
        Objects.requireNonNull(option, "Cfg.Option is null");
        Objects.requireNonNull(value, "value is null");
        String key = option.key();
        map.put(key, value);
        return this;
    }

    /**
     * @param condition 条件，当条件为true，则向配置类设置配置项
     * @param option    配置键, 不能为null
     * @param value     配置值, 不能为null
     * @param <T>       该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException 当给定的配置键为null或给定的配置值为null时
     * @see #reset(Option, Object)
     */
    public <T> Cfg resetIf(boolean condition, Option<T> option, T value) {
        if (condition) {
            reset(option, value);
        }
        return this;
    }

    /**
     * 当给定的value不为null，设置配置项，与{@link #reset(Option, Object)}方法相比，该方法在给定的value为null时不会抛出异常
     *
     * @param option 配置键, 不能为null
     * @param value  配置值
     * @param <T>    该键值对存储的配置值的类型
     * @return this
     * @throws NullPointerException 当给定的配置键为null
     * @see #resetIf(boolean, Option, Object)
     * @deprecated 与 {@link #setIfNotSet(Option, Object)} 有歧义，且用处较小
     */
    @Deprecated(forRemoval = true)
    public <T> Cfg resetIfNotNull(Option<T> option, T value) {
        return resetIf(value != null, option, value);
    }

    /**
     * 从配置类中删除指定配置值和键<br>
     * 可能配置类中并未有指定的键值对，但该方法的删除目的为“确保删除逻辑的最终一致性”，
     * 即传入指定的配置键，该方法调用的最终结果能够确保配置中无指定键值
     *
     * @param option 配置键, 不能为null
     * @return this
     * @throws NullPointerException 当给定的配置键为null时
     * @see #removeIf(boolean, Option)
     */
    public <T> Cfg remove(Option<T> option) {
        Objects.requireNonNull(option, "Cfg.Option is null");
        map.remove(option.key());
        return this;
    }

    /**
     * 当条件成立，从配置类中删除指定配置值和键<br>
     * 可能配置类中并未有指定的键值对，但该方法的删除目的为“确保删除逻辑的最终一致性”，
     * 即传入指定的配置键，该方法调用的最终结果能够确保配置中无指定键值
     *
     * @param condition 条件表达式
     * @param option    配置键, 不能为null
     * @return this
     * @throws NullPointerException 当给定的配置键为null时
     * @see #remove(Option)
     */
    public <T> Cfg removeIf(boolean condition, Option<T> option) {
        if (condition) {
            remove(option);
        }
        return this;
    }


    /**
     * 给定配置键,返回指定的配置键所对应的值<br>
     * 当Cfg中未有该配置键时，则返回给定的otherValue值
     *
     * @param option 配置键, 不能为null
     * @param other  当给定配置键所对应的值不存在时，使用该值作为返回值, 可以使用null作为other
     * @param <T>    配置值类型
     * @return 配置值
     * @throws NullPointerException 当给定的配置键为null时
     */
    public <T> T getOr(Cfg.Option<T> option, T other) {
        return tryGet(option).orElse(other);
    }

    /**
     * 给定配置键,返回指定的配置键所对应的值<br>
     * 当Cfg中未有该配置键时，则执行函数并返回函数执行的返回值
     *
     * @param option     配置键, 不能为null
     * @param fnGetValue 当给定配置键所对应的值不存在时，执行该函数并获取值作为返回值，给定的该函数不能为null
     * @param <T>        配置值类型
     * @return 配置值
     * @throws NullPointerException 当给定的配置键为null时
     */
    public <T> T getOr(Cfg.Option<T> option, Supplier<? extends T> fnGetValue) {
        Objects.requireNonNull(fnGetValue, "fnGetValue is null");
        return tryGet(option).orElseGet(fnGetValue);
    }

    /**
     * 给定配置键,返回Cfg中指定配置键所对应的值<br>
     * 当Cfg中未有该配置键时，则以配置的{@link Option#fallbackOf}顺序找配置键，
     * 当仍未找到该配置键时，该方法不会使用{@link Option}中配置的默认值，而是直接抛出异常
     *
     * @param option 配置键, 不能为null
     * @param <T>    配置值类型
     * @return 配置值
     * @throws NullPointerException         当给定的配置键为null时
     * @throws OptionValueNotFoundException 当给定配置键不在Cfg对象中时
     * @see #tryGet(Option)
     * @see #getOrDefault(Option)
     * @see #tryGetOrDefault(Option)
     */
    public <T> T get(Cfg.Option<T> option) throws OptionValueNotFoundException {
        return tryGet(option)
                .orElseThrow(() -> new OptionValueNotFoundException(option));
    }

    /**
     * 给定配置键，返回Cfg中指定配置键所对应的值的{@link Optional}包装<br>
     * 当Cfg中未有该配置键时，则以配置的{@link Option#fallbackOf}顺序找配置键，
     * 当仍未找到该配置键时，该方法不会使用{@link Option}中配置的默认值，而是直接返回{@link Optional#empty()}
     *
     * @param option 配置键, 不能为null
     * @param <T>    配置值类型
     * @return 配置值Optional
     * @throws NullPointerException 当给定的配置键为null时
     * @see #get(Option)
     * @see #getOrDefault(Option)
     * @see #tryGetOrDefault(Option)
     */
    public <T> Optional<T> tryGet(Cfg.Option<T> option) {
        Objects.requireNonNull(option, "Cfg.Option is null");
        String key = option.key();
        T nullableValue = preciseGetByKey(key);
        if (nullableValue == null) {
            List<Option<T>> fallbackOf = option.fallbackOf;
            if (fallbackOf != null && !fallbackOf.isEmpty()) {
                for (Option<T> fallbackOpt : fallbackOf) {
                    T v = preciseGetByKey(fallbackOpt.key());
                    if (v != null) {
                        nullableValue = v;
                        break;
                    }
                }
            }
        }
        return Optional.ofNullable(nullableValue);
    }

    /**
     * 给定配置键,返回Cfg中指定配置键所对应的值<br>
     * 当Cfg中未有该配置键时，则以配置的{@link Option#fallbackOf}顺序找配置键，
     * 若在该情况下仍未找到配置键，则尝试使用给定的配置键{@link Option}的默认值，若给定的{@link Option}的默认值为null，
     * 则开始以配置的{@link Option#fallbackOf}顺序使用定义的默认值，在仍未找到任意可用的值的情况下，该方法将抛出异常
     *
     * @param option 配置键, 不能为null
     * @param <T>    配置值类型
     * @return 配置值
     * @throws NullPointerException         当给定的配置键为null时
     * @throws OptionValueNotFoundException 当给定配置键不在Cfg对象中，且找不到任何一个非空的默认值时
     * @see #tryGet(Option)
     * @see #get(Option)
     * @see #tryGetOrDefault(Option)
     */
    public <T> T getOrDefault(Cfg.Option<T> option) throws OptionValueNotFoundException {
        return tryGetOrDefault(option)
                .orElseThrow(() -> new OptionValueNotFoundException(option));
    }

    /**
     * 给定配置键,返回Cfg中指定配置键所对应的值，该值以{@link Optional}包装<br>
     * 当Cfg中未有该配置键时，则以配置的{@link Option#fallbackOf}顺序找配置键，
     * 若在该情况下仍未找到配置键，则尝试使用给定的配置键{@link Option}的默认值，若给定的{@link Option}的默认值为null，
     * 则开始以配置的{@link Option#fallbackOf}顺序使用定义的默认值，在仍未找到任意可用的值的情况下，该方法将返回{@link Optional#empty()}
     *
     * @param option 配置键, 不能为null
     * @param <T>    配置值类型
     * @return 配置值Optional
     * @throws NullPointerException 当给定的配置键为null时
     * @see #tryGet(Option)
     * @see #get(Option)
     * @see #getOrDefault(Option)
     */
    public <T> Optional<T> tryGetOrDefault(Cfg.Option<T> option) {
        /*
        20241015：
        经过一段时间使用，发现该方法的一个问题：
        假设Cfg.Option配置了defaultValue 和 failBack
        原有的该方法逻辑为：先从Cfg中找value，当没有value时，使用defaultValue，当没有
        defaultValue时则使用failBack的key寻找，当failBack的key未找到value时，则使用failBack的
        defaultValue...,failBack以此类推....
        这种情况可能不符合该方法提供的逻辑的第一感觉，并且在使用一段时间后发现这种逻辑有一定问题：
        假设某Cfg.Option配置了key为 "1"，defaultValue值为 "defValue1"，
        且其第一个failBack的key为 "2" ...
        当Cfg中有配置key为 "2" 且value为 "noDefValue2" 时，使用该Cfg.Option时，
        将会通过该方法获得值 "defValue1"，而其实通过它的failBack能够从Cfg中找到一个实际配置的值，
        遂在这种情况下，该方法的表意可以说有一定歧义，考虑到该方法已经在部分模块中或其他人的代码中已经使用，
        遂不对方法API做变更，仅对方法逻辑和方法API DOC进行变更，该方法新的逻辑为：
        先通过Cfg.Option和其的failBack从Cfg中找存在的配置值，当找到配置值时，返回配置值
        当未找到配置值时，则开始以同样的顺序从Cfg.Option和其的failBack中找defaultValue，
        若找到defaultValue后，返回找到的第一个可用的非空的defaultValue，否则，该方法返回null
        20250509:
        该方法在整理时语义已重新规划，现能与get()方法以及unsafeGet()方法语义对齐
         */
        T nullableValue = tryGet(option).orElse(null);
        if (nullableValue == null) {
            // get 并且也使用了failBack 还是为null，则开始寻找并使用默认值
            nullableValue = option.defaultValue;
            if (nullableValue == null) {
                // 给定的默认值仍为null，则开始寻找使用failBack的默认值
                List<Option<T>> fallbackOf = option.fallbackOf;
                if (fallbackOf != null && !fallbackOf.isEmpty()) {
                    for (Option<T> fbOption : fallbackOf) {
                        T fbDefaultValue = fbOption.defaultValue;
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
        return Optional.ofNullable(nullableValue);
    }

    /**
     * 向Cfg中批量设定某Map提供的值，若Cfg中已有某键，则该方法抛出异常
     *
     * @param map Map[?:str, ?]
     * @return this
     * @throws NullPointerException        当给定的map为null时
     * @throws OptionKeyDuplicateException 当当前配置类中已有给定的map中的键时
     */
    private Cfg setFromMap(Map<? extends String, ?> map) {
        Objects.requireNonNull(map, "map is null");
        // tod 当key相同，这里会覆盖掉原有的值，遂这里应当进行判断
        map.forEach((othKey, othValue) -> {
            Err.realIf(this.map.containsKey(othKey), () -> new OptionKeyDuplicateException(othKey));
            this.map.put(othKey, othValue);
        });
        return this;
    }

    /**
     * 向Cfg中批量设置某Map提供的值，与{@link #setFromMap(Map)}不同，当Cfg
     * 中已有map中的键时，不会抛出异常
     *
     * @param map Map[?:str, ?]
     * @return this
     * @throws NullPointerException 当给定的map为null时
     */
    private Cfg resetFromMap(Map<? extends String, ?> map) {
        this.map.putAll(map);
        return this;
    }

    /**
     * 实际的直接从map中寻找配置值的方法，该方法内有泛型强转，类型安全由其他调用该方法的方法保证
     *
     * @param key 配置键
     * @param <T> 配置项类型
     * @return 配置值
     */
    @SuppressWarnings("unchecked")
    private <T> T preciseGetByKey(String key) {
        Object nullableValue = map.get(key);
        return (T) nullableValue;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cfg cfg)) return false;
        return map.equals(cfg.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    /**
     * 打印Cfg使用的Map名和其内部的配置项
     *
     * @return Cfg str display
     */
    @Override
    public String toString() {
        return "Cfg[" + map.getClass().getName() + "(" + map + ")]";
    }

    /**
     * 返回迭代器<br>
     * 该迭代器载荷的类型为{@link Tup2}，其中{@link Tup2#l()}引用“配置键”，{@link Tup2#r()}引用“配置值”<br>
     * 该迭代器将委托至该{@link Cfg}内部的{@link #map}的{@link Map#entrySet()}的迭代器，
     * 遂{@link Iterator#remove()}方法可用与否将取决于内部的{@link #map}实现<br>
     *
     * @return 迭代器
     */
    @Override
    public Iterator<Tup2<String, Object>> iterator() {
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        return MappedIter.of(
                it, (en) -> Tup2.of(en.getKey(), en.getValue())
        );
    }

    /**
     * <b>配置项</b><br>
     * 该对象不可变，线程安全，可共享，建议定义至静态字段<br>
     * <pre>
     *     {@code
     *     static final Cfg.Option<Integer> NUMBER = Cfg.Option
     *             .of("number") // requisite
     *             .intType() // requisite
     *             .defaultValue(1)
     *             .description("this is number option desc")
     *             .notFoundValueMsg("not found number option")
     *             .build();
     *     }
     * </pre>
     *
     * @param <T> 配置项值所对应的类型
     */
    public static class Option<T> implements Serializable {
        /**
         * 配置项的“配置键”，同一个{@link Cfg}中不同的{@link Option}配置项标识必须不同
         */
        private final String key;
        /**
         * （可选）配置项默认值，当{@link Cfg}中找不到该配置项的值时，将使用该默认值，该默认值优先级低于{@link Option#fallbackOf}配置项
         */
        private final T defaultValue;

        /**
         * （可选）配置项描述
         */
        private final String description;
        /**
         * （可选）当找不到该配置值时的信息
         */
        private final String notFoundValueMsg;
        /**
         * （可选）当找不到该配置项时，将使用的“回滚”配置项
         */
        private final List<Option<T>> fallbackOf;

        /**
         * 配置项的“配置键”
         *
         * @return 配置项的“配置键”
         */
        public String key() {
            return key;
        }

        /**
         * 配置项默认值，可能为空（未设置的情况下）
         *
         * @return 配置项默认值
         */
        public Optional<T> defaultValue() {
            return Optional.ofNullable(defaultValue);
        }

        /**
         * 配置项描述，可能为空（未设置的情况下）
         *
         * @return 配置项描述
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * 找不到配置值的提示信息，可能为空（未设置的情况下）
         *
         * @return 找不到配置值的提示信息
         */
        public Optional<String> notFoundValueMsg() {
            return Optional.ofNullable(notFoundValueMsg);
        }

        /**
         * “回滚”配置项，可能为空集合(EmptyList)（未设置的情况下）
         *
         * @return “回滚”配置项
         */
        public List<Option<T>> fallbackOf() {
            return fallbackOf;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Option<?> option)) return false;
            return Objects.equals(key, option.key) && Objects.equals(defaultValue, option.defaultValue) && Objects.equals(description, option.description) && Objects.equals(notFoundValueMsg, option.notFoundValueMsg) && Objects.equals(fallbackOf, option.fallbackOf);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, defaultValue, description, notFoundValueMsg, fallbackOf);
        }

        /**
         * 返回当前Option信息
         *
         * @return Option str display
         */
        @Override
        public String toString() {
            return "Cfg.Option(" + key + ")[" +
                   ("defaultValue=" + defaultValue) +
                   (description == null ? "" : ", description='" + description + '\'') +
                   (notFoundValueMsg == null ? "" : ", notFoundValueMsg='" + notFoundValueMsg + '\'') +
                   (fallbackOf == null || fallbackOf.isEmpty() ? "" : ", fallbackOf=" + fallbackOf) +
                   ']';
        }

        private Option(String key, T defaultValue) {
            this(key, defaultValue, null);
        }

        private Option(String key, T defaultValue, String description) {
            this(key, defaultValue, description, null);
        }

        private Option(String key, T defaultValue, String description, String notFoundValueMsg) {
            this(key, defaultValue, description, notFoundValueMsg, Collections.emptyList());
        }

        private Option(String key, T defaultValue, String description, String notFoundValueMsg, List<Option<T>> fallbackOf) {
            if (key == null) {
                throw new NullPointerException("Cfg.Option.key must not be null");
            }
            this.key = key;
            this.defaultValue = defaultValue;
            this.description = description;
            this.notFoundValueMsg = notFoundValueMsg;
            this.fallbackOf = fallbackOf == null ? Collections.emptyList() : fallbackOf;
        }

        /**
         * 创建新配置项，绑定配置项的“配置键”
         * <pre>
         *     {@code
         *     static final Cfg.Option<Boolean> IS_MAN = Cfg.Option
         *             .of("man!")
         *             .booleanType()
         *             .... // setting others...
         *             .build();
         *     }
         * </pre>
         *
         * @param key 配置键
         * @return 配置键绑定的配置项构造器对象
         * @throws NullPointerException 当给定的配置键为空时
         * @see Option
         * @see KeyBindBuilder
         */
        public static KeyBindBuilder of(String key) {
            return new KeyBindBuilder(key);
        }

        /**
         * 配置键绑定的配置项构造器<br>
         * Cfg.Option builder，key绑定，作为Cfg.Option构建过程中的中间对象
         */
        public static class KeyBindBuilder {
            /**
             * 绑定不可变“配置键”
             */
            private final String key;

            private KeyBindBuilder(String key) {
                this.key = key;
            }

            /**
             * （可选）向配置项绑定默认值<br>
             * 配置项要么有默认值，要么没有，在这个设定的意义上，其默认值要么为空(表示无默认值），要么非空，遂不允许设置默认值为“null”<br>
             * 该方法因为入参显式给定了泛型参数，遂类型绑定方法可以省略
             *
             * @param defaultValue 默认值
             * @param <T>          配置项值类型
             * @return 配置项构造器对象
             * @throws NullPointerException 当显式设置"null"为配置项默认值时
             */
            public <T> Builder<T> defaultValue(T defaultValue) {
                Err.realIf(defaultValue == null,
                        NullPointerException::new, "Cfg.Option({}), setting null default value.", key);
                return new Builder<T>(key).defaultValue(defaultValue);
            }

            /**
             * 向配置项绑定配置值类型<br>
             * 该方法将显式的绑定配置项值类型
             * <pre>
             *     {@code
             *     Cfg.Option<List<String>> optSL = Cfg.Option
             *               .of("optSL")
             *               .<List<String>>type()
             *               ...
             *     }
             * </pre>
             *
             * @param <T> 配置项值类型
             * @return 配置项构造器对象
             */
            public <T> Builder<T> type() {
                return new Builder<T>(key);
            }

            /**
             * 向配置项绑定配置值的类型<br>
             * 该方法将显式的绑定配置项值类型
             * <pre>
             *     {@code
             *     Cfg.Option<String> optStr = Cfg.Option
             *               .of("optStr")
             *               .type(String.class)
             *               ...
             *     }
             * </pre>
             *
             * @param type 配置值类
             * @param <T>  配置项值类型
             * @return 配置项构造器对象
             * @see #type()
             */
            public <T> Builder<T> type(Class<T> type) {
                return new Builder<T>(key);
            }

            /**
             * 向配置项绑定配置值的类型<br>
             * 该方法将显式的绑定配置项值类型
             * <pre>
             *     {@code
             *     Cfg.Option<Map<Long, Long>String> optMLL = Cfg.Option
             *               .of("optMLL")
             *               .typeRef(new TypeRef<Map<Long, Long>>{})
             *               ...
             *     }
             * </pre>
             *
             * @param type 配置值类
             * @param <T>  配置项值类型
             * @return 配置项构造器对象
             * @see #type()
             */
            public <T> Builder<T> typeRef(TypeRef<T> type) {
                return new Builder<T>(key);
            }

            /**
             * 将配置值类型绑定为{@link String}
             *
             * @return 配置项构造器对象
             */
            public Builder<String> stringType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Integer}
             *
             * @return 配置项构造器对象
             */
            public Builder<Integer> intType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Long}
             *
             * @return 配置项构造器对象
             */
            public Builder<Long> longType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Float}
             *
             * @return 配置项构造器对象
             */
            public Builder<Float> floatType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Double}
             *
             * @return 配置项构造器对象
             */
            public Builder<Double> doubleType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Boolean}
             *
             * @return 配置项构造器对象
             */
            public Builder<Boolean> booleanType() {
                return new Builder<>(key);
            }

            /**
             * 将配置值类型绑定为{@link Character}
             *
             * @return 配置项构造器对象
             */
            public Builder<Character> charType() {
                return new Builder<>(key);
            }

        }

        /**
         * 配置项构造器<br>
         * Cfg.Option builder 作为Cfg.Option对象构建过程的中间对象
         *
         * @param <T> 配置项的配置值类型
         */
        public static class Builder<T> {

            private final String key;

            private T defaultValue;

            private String description;

            private String notFoundValueMsg;

            private List<Option<T>> fallbackOf;

            private Builder(String key) {
                this.key = key;
            }

            /**
             * （可选）向配置项绑定默认值<br>
             * 配置项要么有默认值，要么没有，在这个设定的意义上，其默认值要么为空(表示无默认值），要么非空，遂不允许设置默认值为“null”
             *
             * @param defaultValue 默认值
             * @return 配置项构造器对象
             * @throws NullPointerException 当显式设置"null"为配置项默认值时
             */
            public Builder<T> defaultValue(T defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            /**
             * （可选）向配置项绑定描述
             *
             * @param description 配置项描述
             * @return this
             */
            public Builder<T> description(String description) {
                this.description = description;
                return this;
            }

            /**
             * （可选）向配置项绑定”找不到值的信息“
             *
             * @param noValueMsg 找不到值的信息
             * @return this
             */
            public Builder<T> notFoundValueMsg(String noValueMsg) {
                this.notFoundValueMsg = noValueMsg;
                return this;
            }

            /**
             * （可选）向配置项绑定”回滚“配置项<br>
             * 当配置项找不到值时，将尝试使用”回滚“配置项寻找值
             *
             * @param fallbackOption ”回滚“配置项
             * @return this
             */
            @SuppressWarnings("UnusedReturnValue")
            public Builder<T> fallbackOf(Option<T> fallbackOption) {
                Objects.requireNonNull(fallbackOption, " given fallbackOption is null");
                if (this.fallbackOf == null) {
                    this.fallbackOf = new ArrayList<>();
                }
                if (!this.fallbackOf.contains(fallbackOption)) {
                    this.fallbackOf.add(fallbackOption);
                }
                return this;
            }

            /**
             * （可选）向配置项绑定”回滚“配置项<br>
             * 当配置项找不到值时，将尝试使用”回滚“配置项寻找值
             *
             * @param fallbackOptions ”回滚“配置项
             * @return this
             */
            @SuppressWarnings("UnusedReturnValue")
            public Builder<T> fallbackOf(List<Option<T>> fallbackOptions) {
                Objects.requireNonNull(fallbackOptions, " given fallbackOptions is null");
                fallbackOptions.forEach(this::fallbackOf);
                return this;
            }

            /**
             * （可选）向配置项绑定”回滚“配置项<br>
             * 当配置项找不到值时，将尝试使用”回滚“配置项寻找值
             *
             * @param fallbackOptions ”回滚“配置项
             * @return this
             */
            @SafeVarargs
            public final Builder<T> fallbackOf(Option<T>... fallbackOptions) {
                Objects.requireNonNull(fallbackOptions, " given fallbackOptions is null");
                fallbackOf(Arrays.asList(fallbackOptions));
                return this;
            }

            /**
             * 构造”配置项“
             *
             * @return 配置项
             */
            public Option<T> build() {
                return new Option<>(key, defaultValue, description, notFoundValueMsg, fallbackOf);
            }
        }


    }

    /**
     * 表示在一个配置类实例中设置{@link Option#key()}重复的配置项
     *
     * @author baifangkual
     * @since 2025/5/10
     */
    static class OptionKeyDuplicateException extends IllegalArgumentException {

        @Serial
        private static final long serialVersionUID = 1919810L;

        OptionKeyDuplicateException(String optionKey) {
            super(buildErrMsgByOptionKey(optionKey));
        }

        OptionKeyDuplicateException(Option<?> option) {
            super(buildErrMsgByOption(option));
        }

        /**
         * 重复配置的异常信息模板
         */
        private static final String EXISTS_MSG_TEMP = "Cfg.Option({}) already exists in Cfg instance";


        static String buildErrMsgByOption(Option<?> option) {
            return buildErrMsgByOptionKey(option.key());
        }

        static String buildErrMsgByOptionKey(String optionKey) {
            return Stf.f(EXISTS_MSG_TEMP, optionKey);
        }
    }

    /**
     * 表示在配置类实例中找不到配置项配置值的异常
     *
     * @author baifangkual
     * @since 2024/6/15 v0.0.3
     */
    public static class OptionValueNotFoundException extends NoSuchElementException {

        @Serial
        private static final long serialVersionUID = 1919810L;

        public OptionValueNotFoundException(Option<?> option) {
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
        static String buildingNotFoundValueMsg(Option<?> option, boolean appendFallbackOptMsg) {
            final String key = option.key();
            final String nullableDescription = option.description()
                    .filter(desc -> !desc.isBlank())
                    .orElse(null);
            final String nullableNoValueMsg = option.notFoundValueMsg()
                    .filter(nvm -> !nvm.isBlank())
                    .orElse(null);
            final Object nullableDefValue = option.defaultValue();

            String msg = Stf.f(NO_VALUE_ERR_MSG_TEMPLATE, key);
            msg = nullableDefValue == null ? msg : msg + Stf.f(APPEND_OPTION_DEFAULT_VALUE, nullableDefValue);
            msg = nullableDescription == null ? msg : msg + Stf.f(APPEND_OPTION_DESCRIPTION, nullableDescription);
            msg = nullableNoValueMsg == null ? msg : msg + Stf.f(APPEND_OPTION_VALUE_NOT_FOUND, nullableNoValueMsg);

            if (!appendFallbackOptMsg) {
                return msg;
            }

            List<? extends Option<?>> fallbackOfOpt = Optional.ofNullable(option.fallbackOf())
                    .filter(fbl -> !fbl.isEmpty())
                    .orElse(null);
            // 为空则直接返回，否则追加fallback的信息
            if (fallbackOfOpt == null) {
                return msg;
            }
            List<String> fallbackOfOptKeys = fallbackOfOpt.stream().map(Option::key).toList();
            msg = msg + Stf.f(APPEND_OPTION_FAIL_BACK, fallbackOfOptKeys);


            for (Option<?> fbOpt : fallbackOfOpt) {
                msg = msg + "\n\n" + buildingNotFoundValueMsg(fbOpt, false);
            }

            return msg;
        }
    }
}
