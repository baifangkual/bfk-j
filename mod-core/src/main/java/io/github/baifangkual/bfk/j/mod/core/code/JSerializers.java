package io.github.baifangkual.bfk.j.mod.core.code;

import java.io.*;
import java.util.Objects;

/**
 * <b>Java Serializers</b><br>
 * java 原生序列化工具类<br>
 * 这是一个简单的工具类，仅来回序列化Java Obj，
 * 而且方法 {@link #ser(Serializable)} 形参引用要求显式的 {@link Serializable} 实现，
 * 该类中方法不考虑各种反序列化的安全性问题，而且序列化的对象不能太大，因为序列化方法返回字节数组
 *
 * @author baifangkual
 * @implNote Java原生序列化（使用ObjectOutputStream）可以处理循环引用。
 * 在序列化过程中，当遇到一个已经被序列化过的对象时，它会使用一种机制（称为"引用共享"）来避免无限循环和重复数据。
 * 具体来说：
 * 1. 第一次序列化一个对象时，会为其分配一个唯一的序列号（serial number），并将对象内容写入流。
 * 2. 当再次遇到同一个对象（即循环引用）时，不会再次序列化该对象的内容，而是写入一个特殊的标记（TC_REFERENCE）后跟该对象的序列号。
 * 这样，在反序列化时，当读取到一个引用标记，就会根据序列号去查找之前已经反序列化的对象，从而重建引用关系。
 * 因此，循环引用不会导致无限循环，也不会导致栈溢出。但是，需要注意的是：
 * - 这种引用共享机制是默认开启的，而且对于同一个对象（即同一个内存地址）才会被识别为同一个对象。
 * - 如果你不希望共享同一个对象（例如，希望每次出现都序列化为独立的对象），
 * 可以使用`ObjectOutputStream.writeUnshared()`方法，但这样在遇到循环引用时就会导致问题（比如栈溢出）。
 * @since 2024/12/5 v0.0.7
 */
public class JSerializers {
    private JSerializers() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * 序列化对象为字节数组
     *
     * @param obj 需序列化的对象（必须实现Serializable接口）
     * @return 对象的字节序列表示
     * @throws SerializeException 如果序列化失败或对象未实现Serializable
     */
    public static byte[] ser(Serializable obj) {
        Objects.requireNonNull(obj);
        try (ByteArrayOutputStream bout = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bout)) {
            out.writeObject(obj);
            out.flush(); // 写完，虽然在close时会自动调用，toByteArray过程在try内
            return bout.toByteArray();
        } catch (IOException e) {
            throw new SerializeException("Serialization failed:: " + e.getMessage(), e);
        }
    }

    /**
     * 从字节数组反序列化对象
     * <pre>
     *     {@code
     *     Obj obj = ...;
     *     byte[] bytes = JSers.seri(obj);
     *     Obj deSerObj1 = JSers.deSeri(bytes);
     *     Obj deSerObj2 = JSers.<Obj>deSeri(bytes);
     *     }
     * </pre>
     *
     * @param bytes 序列化后的字节数组
     * @return 反序列化的对象
     * @throws DeserializeException 如果反序列化失败
     */
    @SuppressWarnings("unchecked")
    public static <T> T deSer(byte[] bytes) {
        Objects.requireNonNull(bytes);
        try (ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
             ObjectInputStream in = new ObjectInputStream(bin)) {
            return (T) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new DeserializeException("Deserialization failed: " + e.getMessage(), e);
        }
    }


    private static class DeserializeException extends IllegalStateException {
        public DeserializeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class SerializeException extends IllegalStateException {
        public SerializeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
