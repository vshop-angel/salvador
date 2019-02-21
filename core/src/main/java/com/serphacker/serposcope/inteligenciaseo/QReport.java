package com.serphacker.serposcope.inteligenciaseo;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

import javax.annotation.Generated;
import java.sql.Types;

@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QReport extends RelationalPathBase<QReport> {
    private static final long serialVersionUID = -1996639244;
    public static final QReport t_report = new QReport("IS_REPORTS");

    NumberPath<Integer> id = createNumber("ID", Integer.class);
    NumberPath<Integer> groupId = createNumber("GROUP_ID", Integer.class);
    StringPath name = createString("NAME");
    StringPath iframe = createString("IFRAME");

    public QReport(String variable) {
        super(QReport.class, variable, "PUBLIC", "IS_REPORTS");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(groupId, ColumnMetadata.named("GROUP_ID").withIndex(2).ofType(Types.INTEGER).withSize(10));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(iframe, ColumnMetadata.named("IFRAME").withIndex(3).ofType(Types.VARCHAR).withSize(255));
    }
}
