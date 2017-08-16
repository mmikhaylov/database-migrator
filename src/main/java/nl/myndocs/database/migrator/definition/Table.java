package nl.myndocs.database.migrator.definition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by albert on 13-8-2017.
 */
public class Table {
    private String tableName;
    private List<Column> newColumns = new ArrayList<>();
    private Collection<ForeignKey> newForeignKeys = new ArrayList<>();
    private Collection<Column> changeColumns = new ArrayList<>();
    private Collection<String> dropForeignKeys = new ArrayList<>();
    private Collection<String> dropColumns = new ArrayList<>();

    private Table(Builder tableBuilder) {
        tableName = tableBuilder.getTableName();

        long emptyTypeCount = tableBuilder.getNewColumns()
                .stream()
                .filter(column -> column.getType() == null)
                .count();

        if (emptyTypeCount > 0) {
            throw new RuntimeException("New column types should have a type");
        }

        tableBuilder.getNewColumns()
                .forEach(column -> newColumns.add(column.build()));

        tableBuilder.getNewForeignColumnKeys()
                .forEach(foreignColumnKey -> newForeignKeys.add(foreignColumnKey.build()));

        tableBuilder.getChangeColumns()
                .forEach(column -> changeColumns.add(column.build()));

        dropColumns = new ArrayList<>(tableBuilder.getDropColumns());
        dropForeignKeys = new ArrayList<>(tableBuilder.getDropForeignKey());
    }

    public String getTableName() {
        return tableName;
    }

    public List<Column> getNewColumns() {
        return newColumns;
    }

    public Collection<Column> getChangeColumns() {
        return changeColumns;
    }

    public Collection<ForeignKey> getNewForeignKeys() {
        return newForeignKeys;
    }

    public Collection<String> getDropForeignKeys() {
        return dropForeignKeys;
    }

    public Collection<String> getDropColumns() {
        return dropColumns;
    }

    public static class Builder {
        private String tableName;
        private List<Column.Builder> newColumnBuilders = new ArrayList<>();
        private List<Column.Builder> changeColumns = new ArrayList<>();
        private Collection<ForeignKey.Builder> newForeignColumnKeys = new ArrayList<>();
        private Collection<String> dropForeignKey = new ArrayList<>();
        private Collection<String> dropColumns = new ArrayList<>();

        public Builder(String tableName) {
            this.tableName = tableName;
        }

        private Column.Builder addNewColumn(String columnName, Column.TYPE type) {
            Column.Builder builder = new Column.Builder(columnName, type);

            newColumnBuilders.add(builder);
            return builder;
        }

        public Table.Builder addColumn(String columnName, Column.TYPE type) {
            addNewColumn(columnName, type);

            return this;
        }

        public Table.Builder addColumn(String columnName, Column.TYPE type, Consumer<Column.Builder> column) {
            Column.Builder columnBuilder = addNewColumn(columnName, type);
            column.accept(columnBuilder);

            return this;
        }

        private Column.Builder addChangeColumn(String columnName) {
            Column.Builder builder = new Column.Builder(columnName);

            changeColumns.add(builder);
            return builder;
        }

        public Table.Builder changeColumn(String columnName) {
            addChangeColumn(columnName);

            return this;
        }

        public Table.Builder changeColumn(String columnName, Consumer<Column.Builder> column) {
            Column.Builder columnBuilder = addChangeColumn(columnName);
            column.accept(columnBuilder);

            return this;
        }

        private ForeignKey.Builder createNewForeignKey(String constraintName, String foreignTable, Collection<String> localKeys, Collection<String> foreignKeys) {
            ForeignKey.Builder builder = new ForeignKey.Builder(constraintName, foreignTable, localKeys, foreignKeys);
            newForeignColumnKeys.add(
                    builder
            );

            return builder;
        }


        public Builder addForeignKey(String constraintName, String foreignTable, Collection<String> localKeys, Collection<String> foreignKeys) {
            createNewForeignKey(constraintName, foreignTable, localKeys, foreignKeys);

            return this;
        }

        public Builder addForeignKey(String constraintName, String foreignTable, String localKey, String foreignKey) {
            return addForeignKey(constraintName, foreignTable, Arrays.asList(localKey), Arrays.asList(foreignKey));
        }

        public Builder addForeignKey(String constraintName, String foreignTable, Collection<String> localKeys, Collection<String> foreignKeys, Consumer<ForeignKey.Builder> foreignKeyConsumer) {
            foreignKeyConsumer.accept(
                    createNewForeignKey(constraintName, foreignTable, localKeys, foreignKeys)
            );
            return this;
        }

        public Builder addForeignKey(String constraintName, String foreignTable, String localKey, String foreignKey, Consumer<ForeignKey.Builder> foreignKeyConsumer) {
            foreignKeyConsumer.accept(
                    createNewForeignKey(constraintName, foreignTable, Arrays.asList(localKey), Arrays.asList(foreignKey))
            );

            return this;
        }

        public Builder dropForeignKey(String constraintName) {
            this.dropForeignKey.add(constraintName);

            return this;
        }

        public Builder dropColumn(String columnName) {
            this.dropColumns.add(columnName);

            return this;
        }

        public Collection<String> getDropForeignKey() {
            return dropForeignKey;
        }

        public Collection<String> getDropColumns() {
            return dropColumns;
        }

        public Collection<ForeignKey.Builder> getNewForeignColumnKeys() {
            return newForeignColumnKeys;
        }

        public String getTableName() {
            return tableName;
        }

        public List<Column.Builder> getNewColumns() {
            return newColumnBuilders;
        }

        public List<Column.Builder> getChangeColumns() {
            return changeColumns;
        }

        public Table build() {
            return new Table(this);
        }
    }
}
