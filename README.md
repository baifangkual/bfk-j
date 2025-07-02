# bfk-j 项目说明

总结工作中写过的 java 代码并提取部分可重用的代码
，将部分工作过程中一些 java 代码存放到该项目模块中,
可供后续使用。部分测试代码也将在该项目下测试。

## 设计约束

* public等对外API方法函数中，不允许返回null值表达可能为空的结果，所有可能为null的值均应使用Optional<T>
  表达，若有例外，则应当在方法说明文档中予以说明
    * 因为java中Optional包装有开销，遂如果可以，最好还提供：
        * （1）返回boolean的查询方法——以某种形式描述可预期的Optional结果、
        * （2）返回非Optional包装的方法，这些方法永远不应当返回null，而是在返回null的情况前抛出异常（调用方应以联动返回boolean的查询方法使用）
    * 对返回同一逻辑实体的方法，可选有三种分类的方法签名形容该方法性质（`XXX tryXXX unsafeXXX`
      ），根据方法实际逻辑及性质，这三分类方法签名可组合对应返回值的形式：
        * （1）返回值或直接抛出异常（不得返回null）、
        * （2）返回Optional包装、
        * （3）返回Result包装
* public等对外API方法函数中，默认不允许入参为null值，若有例外，则应当在方法说明文档中予以说明
* 提供的类、实体等，应说明其可变性、线程安全性等
* 在设计时优先考虑无状态对象、次之为状态不可变对象、再次为有状态对象（因缺陷，java状态不可变包括直接引用的实例地址、值不可变，间接引用的实例地址、值可变的情况）
* 在设计时应考虑其是否Serializable

## 模块简述

* bfk-j 最顶层的模块、控制各模块间依赖及版本等
    * jlib-core 包含最常用的、核心的可复用代码，涵盖配置类、容器对象、工具类等
    * [jlib-vfs](./jlib-vfs/README.md) 包含虚拟文件系统相关代码
        * jlib-vfs-api 包含虚拟文件系统api接口等相关代码
        * jlib-vfs-impl-ftp 包含对ftp的实现（依赖apache-commons-net）
        * jlib-vfs-impl-smb 包含对smb的实现（依赖smbj）
        * jlib-vfs-impl-minio 包含对minio的实现（依赖minio）
    * [jlib-db](./jlib-db/README.md) 包含部分数据库操作及元数据的相关代码

## 使用

该项目下代码以库形式发布在 [中央仓库](https://central.sonatype.com/namespace/io.github.baifangkual)

各模块最低兼容 `java17`，按需引入

该项目版本遵循一般约定，在版本到达 `1.0.0` 之前，不保证API稳定性

```xml

<dependencies>
    <!-- 核心模块 -->
    <dependency>
        <groupId>io.github.baifangkual</groupId>
        <artifactId>jlib-core</artifactId>
        <version>0.1.2</version>
    </dependency>
    <!-- 数据库操作 -->
    <dependency>
        <groupId>io.github.baifangkual</groupId>
        <artifactId>jlib-db</artifactId>
        <version>0.1.2</version>
    </dependency>
    <!-- ftp 操作 -->
    <dependency>
        <groupId>io.github.baifangkual</groupId>
        <artifactId>jlib-vfs-impl-ftp</artifactId>
        <version>0.1.2</version>
    </dependency>
    <!-- smb 操作 -->
    <dependency>
        <groupId>io.github.baifangkual</groupId>
        <artifactId>jlib-vfs-impl-smb</artifactId>
        <version>0.1.2</version>
    </dependency>
    <!-- minio 操作 -->
    <dependency>
        <groupId>io.github.baifangkual</groupId>
        <artifactId>jlib-vfs-impl-minio</artifactId>
        <version>0.1.2</version>
    </dependency>
</dependencies>
```

## License

Copyright (c) baifangkual

[MIT] OR [Apache-2.0]

[MIT]: ./LICENSE-MIT
[Apache-2.0]: ./LICENSE-APACHE
