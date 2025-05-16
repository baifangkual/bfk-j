package io.github.baifangkual.bfk.j.mod.vfs.minio.action;

/**
 * minio api 提供的文件夹行为
 * 参见 <a href="https://min.io/docs/minio/linux/developers/java/API.html#putObject">minio.putObj</a>
 * <pre>{@code
 * // Create object ends with '/' (also called as folder or directory).
 * minioClient.putObject(
 *     PutObjectArgs.builder().bucket("my-bucketname").object("path/to/")
 *     .stream(new ByteArrayInputStream(new byte[] {}), 0, -1).build());}</pre>
 * <ul>
 *     <li>使用该方式创建的文件夹，创建的文件夹实体内部会有同名文件夹，该内部的同名文件夹无属性</li>
 *     <li>该方式创建的文件夹，listObject其，能够看见内部实体，且该内部实体objectName非实体本身名字，而是被创建的文件夹的名字</li>
 *     <li>该minio原生的通过Cli的API创建的文件夹中这个行为异常的标志实体的listObject返回值中
 *     isDir属性值为false，但其ObjectName以 “/”结尾，遂可以通过该点判断其</li>
 * </ul>
 *
 * @author baifangkual
 * @since 2024/9/5 v0.0.5
 */
public class MinioAPIDirectoryAction {

    /*
    因为minio的特殊性，删除文件夹内所有内容后，该文件夹也会自行消失，遂仅删除引用即可
    20240906：经测试，发现，通过minio API 官方示例中方式所创建的 “文件夹”，其内部会生成被创建的“文件夹”的同名“文件夹”
    这个生成的内部的同名实体，有如下性质：其在 listObjects 结果中可见，其 listObjects 结果中，ObjectName属性为外侧被创建的“文件夹”的ObjectName，
    其 isDir 属性结果为false，但从minio ui前端可见其为文件夹图标，且前端无法浏览其属性等
    20240906: 经测试，可见，删除通过 minio API创建的“文件夹” 内部 同名的 “文件夹”后，该被创建的”文件夹“将回到minio原始文件夹的行为（即内部有文件便存在，
    内部无文件便消失），也许可以利用这点创建实际的文件夹？
    20240906: 经测试，可知，cli.removeObject行为表达结果一致性
    tag：后续应实现该作为minio文件夹的默认行为
     */
}
