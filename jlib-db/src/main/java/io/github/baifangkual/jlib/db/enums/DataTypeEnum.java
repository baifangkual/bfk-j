package io.github.baifangkual.jlib.db.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 *  jdbc-ResultSet-MetaData的数据类型映射
 */
@Getter
@AllArgsConstructor
public enum DataTypeEnum {

    // Boolean
    BIT(-7, "byte", List.of("BIT")),

    // 整型
    TINYINT(-6, "java.lang.Integer", List.of("TINYINT")),
    SMALLINT(5,"java.lang.Integer",List.of("smallint")),
    INT(4, "java.lang.Integer", List.of("INT")),
    BIGINT(-5, "java.lang.Long", List.of("BIGINT")),

    // 小数 float、double
    FLOAT(6, "java.lang.Float", List.of("FLOAT")),
    DOUBLE(8, "java.lang.Double", List.of("DOUBLE")),
    // DECIMAL和NUMERIC都是用于存储精确数值的数据类型，DECIMAL使用十进制算术运算，‌适用于货币和需要精确计算的场景。‌NUMERIC使用二进制算术运算，‌适用于需要高度精确和快速计算的场景。‌
    // mysql无NUMERIC,可选择，但是直接保存为DECIMAL
    NUMERIC(2,"java.lang.Double",List.of("NUMERIC")),
    DECIMAL(3,"java.lang.Double",List.of("DECIMAL")),
    // 单精度浮点数，能精确到6-7位，mysql无，可选择，但直接保存为double
    REAL(7,"java.lang.Double",List.of("real")),

    // -1 就是 text, string
    CHAR(1, "java.lang.String", List.of("char")),
    VARCHAR(12, "java.lang.String", List.of("VARCHAR")),
    // 大文本一般建议以 数据流读取，此处以字符串读取 mysql无LONGVARCHAR
    LONGTEXT(-1, "java.lang.String", List.of("TEXT","LONGTEXT/LONGVARCHAR")),
    // BINARY和VARBINARY类型类似于CHAR和VARCHAR，不同的是它们包含二进制字节字符串
    BINARY(-2,"byte[]", List.of("binary")),
    VARBINARY(-3,"byte[]", List.of("varbinary")),
    // mysql没找到
    LONGVARBINARY(-4,"java.io.InputStream", List.of()),

    // sql.Date 与 sql.Timestamp
    DATE(91,"java.sql.Date", List.of("DATE")),
    TIMESTAMP(93,"java.sql.Timestamp", List.of("TIMESTAMP","DATETIME")),
    TIME(92,"java.sql.Time", List.of("TIME")),

    // mysql无，暂时未知
    NULL(0,"",List.of()),
    // 泛型，暂时未知使用
    OTHER(1111,"java.lang.String",List.of("other")),
    ;


    final private int dataType;

    final private String javaDataType;

    final private List<String> sqlDataType;


    public static DataTypeEnum getDataTypeEnum(Integer dataType) {
        for (DataTypeEnum dataTypeEnum : DataTypeEnum.values()) {
            if (dataType.equals(dataTypeEnum.getDataType())) {
                return dataTypeEnum;
            }
        }
        return null;
    }

}
