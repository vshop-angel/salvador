package com.serphacker.serposcope.inteligenciaseo;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

import javax.annotation.Generated;
import java.sql.Types;

@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QSearchSettings extends RelationalPathBase<QSearchSettings> {
    private static final long serialVersionUID = -1996639245;
    public static final QSearchSettings searchSettings = new QSearchSettings("IS_SEARCH_SETTINGS");

    public NumberPath<Integer> id = createNumber("ID", Integer.class);
    public NumberPath<Integer> groupId = createNumber("GROUP_ID", Integer.class);
    public NumberPath<Integer> searchId = createNumber("SEARCH_ID", Integer.class);
    public StringPath category = createString("CATEGORY");
    public StringPath volume = createString("VOLUME");
    public BooleanPath adminsOnly = createBoolean("ADMINS_ONLY");

    public QSearchSettings(String variable) {
        super(QSearchSettings.class, variable, "PUBLIC", "IS_SEARCH_SETTINGS");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).notNull());
        addMetadata(searchId, ColumnMetadata.named("SEARCH_ID").withIndex(3).ofType(Types.INTEGER).notNull());
        addMetadata(category, ColumnMetadata.named("CATEGORY").withIndex(4).ofType(Types.VARCHAR).withSize(100).notNull());
        addMetadata(volume, ColumnMetadata.named("VOLUME").withIndex(5).ofType(Types.VARCHAR).withSize(100).notNull());
        addMetadata(adminsOnly, ColumnMetadata.named("ADMINS_ONLY").withIndex(6).ofType(Types.BOOLEAN).notNull());
    }
}
