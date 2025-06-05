package io.github.baifangkual.jlib.core.ref;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * <b>类型引用</b><br>
 * 表达一个java中的类型，允许表达参数化类型<br>
 * <pre>
 *     {@code
 *     TypeRef<List<String>> lsr = new TypeRef<>() {};
 *     Assertions.assertEquals("java.util.List<java.lang.String>", lsr.type().toString());
 *     Assertions.assertEquals("TypeRef<java.util.List<java.lang.String>>", lsr.toString());
 *     TypeRef<Map<String, Integer>> msi = new TypeRef<>() {};
 *     Assertions.assertEquals("java.util.Map<java.lang.String, java.lang.Integer>", msi.type().toString());
 *     Assertions.assertEquals("TypeRef<java.util.Map<java.lang.String, java.lang.Integer>>", msi.toString());
 *     }
 * </pre>
 *
 * @param <T> 携带的类型
 * @author baifangkual
 * @implNote 因为java.Class无法表示参数化类型，遂参考jackson等，有此类，表示携带泛型参数的类型<br>
 * @since 2024/6/18 v0.0.3
 */
public abstract class TypeRef<T> implements Type, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    protected final Type rawType;

    protected TypeRef() {
        this.rawType = getSuperclassTypeParameter(getClass());
    }

    /**
     * 获取自身参数化类型信息
     *
     * @param clazz cls self
     * @return 参数化类型信息
     */
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
        //noinspection UnnecessaryLocalVariable
        Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
        /*
        不再深入找
        if (rawType instanceof ParameterizedType) {
            rawType = ((ParameterizedType) rawType).getRawType();
        }
         */
        return rawType;
    }

    /**
     * 返回携带参数化类型的类型信息
     *
     * @return 携带参数化类型的类型信息
     */
    public Type type() {
        return rawType;
    }

    @Override
    public String toString() {
        return "TypeRef<" + rawType.toString() + ">";
    }
}
