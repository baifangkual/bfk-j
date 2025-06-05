package io.github.baifangkual.jlib.db.entities;

import io.github.baifangkual.jlib.db.enums.DataTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class SQLColumn implements Serializable {

    private String colName;

    private DataTypeEnum colType;

}
