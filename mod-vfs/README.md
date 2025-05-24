## 概述

虚拟文件系统操作工具集

该模块作为工具集，对调用方提供统一的API和接口等

## 使用

```java
// 构建或从他处转换至参数对象
Cfg cfg = Cfg.of("...");
// 构建 虚拟文件系统
VFS vfs = VFSFactory.of(vfsType).build(cfg);
```

VFS、VPath、VFile的关系和操作等，查看相应的类文档：

* io.github.baifangkual.bfk.j.mod.vfs.VFS
* io.github.baifangkual.bfk.j.mod.vfs.VFile
* io.github.baifangkual.bfk.j.mod.vfs.VPath
* io.github.baifangkual.bfk.j.mod.vfs.VFSFactory

该模块提供对多种资源文件的访问方法：

* FTP(依赖apache-commons-net)
    * io.github.baifangkual.bfk.j.mod.vfs.ftp.conf.FTPCfgOptions
    * io.github.baifangkual.bfk.j.mod.vfs.ftp.FTPVirtualFileSystem
* SMB(依赖smbj)
    * io.github.baifangkual.bfk.j.mod.vfs.smb.conf.SMBCfgOptions
    * io.github.baifangkual.bfk.j.mod.vfs.smb.SMBShareRootVirtualFileSystem
* MINIO(依赖minio)
    * io.github.baifangkual.bfk.j.mod.vfs.minio.conf.MinioCfgOptions
    * io.github.baifangkual.bfk.j.mod.vfs.minio.MinioBucketRootVirtualFileSystem
* LOCAL(not impl)
* HDFS(not impl)
* ...

## 实现要求

* 文件系统实例不可序列化
* 文件系统实例为有状态对象，使用完成需关闭
* 优先完成非线程安全实例的实现
* 所有非必要提供的连接配置应设定其默认值甚至常用值
* 提供对虚拟文件系统中目录的浏览
* 提供文件流，读写，创建文件
* 提供创建文件夹
* 提供删除文件或文件夹
* 提供文件系统中文件及文件夹的存在与否查询

## VFS子模块开发

* 子模块需通过java spi实现`io.github.baifangkual.bfk.j.mod.vfs.VFSFactory`, 完成实现类的服务提供和发现
* 子模块需实现`io.github.baifangkual.bfk.j.mod.vfs.VFS`接口的所有必要方法，在已知部分行为的情况下，可拓展覆盖部分默认方法行为

## 额外

* 应考虑smb1协议的支持吗？或许较古早的windows仅支持smb1，当为该情况，应降级
* 查看微软文档https:
  //learn.microsoft.com/zh-cn/windows-server/storage/file-server/troubleshoot/detect-enable-and-disable-smbv1-v2-v3
  发现似乎win7支持smb2，那就先用smb2实现
* jcifs有文档https://www.jcifs.org/ 但是太久没维护，找不到api文档，jcifs-ng在jcifs上维护，但是也没有api文档，遂优先使用smbj
* smbj项目，低级实现，https://github.com/hierynomus/smbj
    * 消息STATUS错误表参见smb/cifs标准 https://msdn.microsoft.com/en-us/library/cc704588.aspx
      及规范 https://msdn.microsoft.com/en-us/library/cc246482.aspx
    * STATUS_ACCESS_DENIED 即不允许当前用户访问请求的文件，该行为应该有相应异常体表示
    * 应该开个口子表示请求文件的行为“掩码”，防止频繁的 STATUS_ACCESS_DENIED，例：在读文件时，应仅声明最小的读掩码
      FILE_READ_DATA
    * 特殊的MAXIMUM_ALLOWED值可用于要求服务器授予访问控制列表允许的全部权限。然后可以查询FileServer
      Information信息类以确定服务器授予了哪组权限。
    * STATUS_SHARING_VIOLATION 共同访问一个文件的相关问题
    * smbj内client -> conn -> session -> share 依次创建，最外侧client关闭将会将内侧关闭，遂可能不需要主动关闭内侧？
* smb各项消息需参考微软官方文档 https://learn.microsoft.com/en-us/windows/win32/fileio/creating-and-opening-files
* smb
  fileAttr消息结构 https://learn.microsoft.com/zh-cn/openspecs/windows_protocols/ms-smb/65e0c225-5925-44b0-8104-6b91339c709f
* minio的“文件夹”概念，minio无所谓文件夹概念，仅有前缀，其SDK API
  listObjects方法要求给定查询前缀，此前缀确实是真前缀，因为给定 'abc/de' 甚至能查询出 'abc/def'
* minio的文件夹行为策略弄成可选项，给定默认选项，不同选项文件夹行为不同

---
最后编辑于20250517，bfk
