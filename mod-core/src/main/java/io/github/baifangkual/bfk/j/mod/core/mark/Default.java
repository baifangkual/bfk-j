package io.github.baifangkual.bfk.j.mod.core.mark;

import io.github.baifangkual.bfk.j.mod.core.lang.Tup2;
import io.github.baifangkual.bfk.j.mod.core.ref.TypeRef;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * <b>标记默认实例</b><br>
 * 实现该接口的类型拥有默认实体（从类型引用可访问的实体）<br>
 * 实现该接口的类型通常拥有某种形式的无参构造，可以获取到“默认实体”的引用，无论调用多少次其给定的返回默认实体的引用的方法，
 * 其返回值（默认实体引用值）不应发生变化
 *
 * @author baifangkual
 * @see #get(Class)
 * @since 2025/5/24 v0.0.7
 */
@Deprecated
public interface Default {

    // todo deprecated this...
    //  这种通过javaSPI寻找提供者的方式已测试可行，
    //  即：SPI Stream（不加载实例，但可以获取到Class） + Anno（因为可以获取到Class，则可通过反射获取到Anno信息）
    //  但达不到想要的效果：
    //  Default可以处理一般不携带泛型的类型的实例，但defaultInstance有泛型信心，则得通过TypeRef
    //  这样的方式太丑：Default.get(new TypeRef<List<String>>(){}) ...
    //  遂该类需被废弃并完全删除，不过这次实践蛮有意思...

    /**
     * 给定某实现 {@link Default} 的类型，返回其默认实体
     *
     * @param cls 实现 {@link Default} 的类型
     * @param <T> 默认实体类型
     * @return 默认实体
     */
    static <T> T get(Class<T> cls) {
        Objects.requireNonNull(cls, "cls must not be null");
        return DefaultInstanceHolder.getByFullName(cls.getName());
    }

    static <T> T get(TypeRef<T> typeRef) {
        Objects.requireNonNull(typeRef, "typeRef must not be null");
        return DefaultInstanceHolder.getByFullName(typeRef.toString());
    }

    /**
     * default provider
     * 默认实例提供者（类型），应用该注解标注类型，
     * 并至少声明 {@link #method()} 标识 静态无参 方法向外界提供的实体的引用
     */
    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface prov {
        /**
         * 标记 静态无参方法 的 方法名<br>
         * 该方法仅会被调用一次以获取默认实体
         *
         * @return 静态无参方法-方法名
         */
        String method();

        /**
         * 携带泛型的类型，借助 {@link TypeRef#toString()} 生成
         *
         * @return 泛型参数
         */
        String genericType() default "";
    }
}

class DefaultInstanceHolder {
    static final Map<String, Object> fullNameRefObjs = new HashMap<>();
    static final Set<String> notFoundRefs = new HashSet<>();

    static {
        ServiceLoader.load(Default.class)
                .stream()
                .map(ServiceLoader.Provider::type)
                .filter(cls -> !cls.isInterface())
                .forEach(cls -> {
                    Tup2<String, Object> tup2 = loadFromAnno(cls);
                    if (tup2 != null) {
                        fullNameRefObjs.put(tup2.l(), tup2.r());
                    }
                });

    }

    @SuppressWarnings("unchecked")
    static <V> V getByFullName(String fullName) {
        return (V) fullNameRefObjs.get(fullName);
    }

    static Tup2<String, Object> loadFromAnno(Class<?> cls) {
        if (cls == null) return null;
        Default.prov provTag = cls.getAnnotation(Default.prov.class);
        if (provTag == null) return null;
        String mName = provTag.method();
        if (mName == null || mName.isBlank()) return null; // 故意标记了空字符串或blank字符串的排除掉
        try {
            Method methodOfProvider = cls.getDeclaredMethod(mName);
            methodOfProvider.setAccessible(true);
            Object defaultInstance = methodOfProvider.invoke(null);
            if (defaultInstance == null) return null;
            String genericType = provTag.genericType();
            if (genericType == null || genericType.isBlank()) {
                return Tup2.of(cls.getName(), defaultInstance);
            } else {
                // 泛型参数非空，则存储泛型参数类型
                return Tup2.of(genericType, defaultInstance);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return null;
        }
    }

}
