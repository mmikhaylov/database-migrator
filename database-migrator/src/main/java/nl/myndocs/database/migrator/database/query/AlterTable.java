package nl.myndocs.database.migrator.database.query;

import nl.myndocs.database.migrator.definition.Column;
import nl.myndocs.database.migrator.definition.Constraint;
import nl.myndocs.database.migrator.definition.Index;

/**
 * Created by albert on 20-8-2017.
 */
public interface AlterTable {

    AlterColumn alterColumn(Column column);

    void addColumn(Column column);

    void dropColumn(String columnName);

    void addConstraint(Constraint constraint);

    void dropConstraint(String constraintName);

    void addIndex(Index index);

    void dropIndex(String indexName);
}
