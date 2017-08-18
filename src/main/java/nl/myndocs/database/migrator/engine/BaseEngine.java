package nl.myndocs.database.migrator.engine;

import nl.myndocs.database.migrator.definition.Column;
import nl.myndocs.database.migrator.definition.Constraint;
import nl.myndocs.database.migrator.definition.ForeignKey;
import nl.myndocs.database.migrator.definition.Table;
import nl.myndocs.database.migrator.engine.exception.CouldNotProcessException;
import nl.myndocs.database.migrator.engine.query.Phrase;
import nl.myndocs.database.migrator.engine.query.Query;
import nl.myndocs.database.migrator.validator.TableValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nl.myndocs.database.migrator.engine.query.Phrase.*;

/**
 * Created by albert on 17-8-2017.
 */
public abstract class BaseEngine implements Engine {

    private static final String ALTER_TABLE_ALTER_DEFAULT = "ALTER TABLE %s ALTER COLUMN %s SET DEFAULT %s";

    private final Connection connection;
    private Map<Phrase, Function<Query, String>> phrasesMap = new HashMap<>();

    public BaseEngine(Connection connection) {
        this.connection = connection;
        phrasesMap.put(Phrase.ALTER_TABLE, query -> "ALTER TABLE " + query.getTable().getTableName());
        phrasesMap.put(Phrase.ALTER_COLUMN, query -> "ALTER COLUMN " + query.getColumn().getColumnName());
        phrasesMap.put(Phrase.SET_DEFAULT, query -> "SET DEFAULT '" + query.getColumn().getDefaultValue().get() + "'");
        phrasesMap.put(Phrase.RENAME, query -> "RENAME TO " + query.getColumn().getRename().get());
        phrasesMap.put(Phrase.TYPE, query -> getNativeColumnDefinition(query.getColumn()));
        phrasesMap.put(Phrase.DROP_COLUMN, query -> "DROP COLUMN " + query.getColumnName());
        phrasesMap.put(Phrase.DROP_FOREIGN_KEY, query -> "DROP CONSTRAINT " + query.getConstraintName());
        phrasesMap.put(Phrase.DROP_CONSTRAINT, query -> "DROP CONSTRAINT " + query.getConstraintName());
        phrasesMap.put(Phrase.ADD_CONSTRAINT, query -> {
            Constraint constraint = query.getConstraint();
            return String.format(
                    "ADD CONSTRAINT %s %s (%s)",
                    constraint.getConstraintName(),
                    getNativeConstraintType(constraint.getType().get()),
                    String.join(",", constraint.getColumnNames())
            );
        });
        phrasesMap.put(Phrase.ADD_FOREIGN_KEY, query -> {
            ForeignKey foreignKey = query.getForeignKey();
            StringBuilder alterForeignKeyQueryBuilder = new StringBuilder();

            alterForeignKeyQueryBuilder.append(
                    String.format(
                            "ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)",
                            foreignKey.getConstraintName(),
                            String.join(",", foreignKey.getLocalKeys()),
                            foreignKey.getForeignTable(),
                            String.join(",", foreignKey.getForeignKeys())
                    )
            );

            if (foreignKey.getDeleteCascade().isPresent()) {
                alterForeignKeyQueryBuilder.append(" ON DELETE " + getNativeCascadeType(foreignKey.getDeleteCascade().get()));
            }

            if (foreignKey.getUpdateCascade().isPresent()) {
                alterForeignKeyQueryBuilder.append(" ON UPDATE " + getNativeCascadeType(foreignKey.getUpdateCascade().get()));
            }

            return alterForeignKeyQueryBuilder.toString();
        });
        phrasesMap.put(Phrase.CREATE_TABLE, query -> {
            Table table = query.getTable();

            List<String> createColumnQueries = table.getNewColumns()
                    .stream()
                    .map(column -> getQueries(
                            new Query()
                                    .query(ADD_COLUMN)
                                    .setColumn(column)
                            )
                    )
                    .flatMap(queries -> Arrays.asList(queries).stream())
                    .collect(Collectors.toList());

            return String.format(
                    "CREATE TABLE %s (%s)",
                    table.getTableName(),
                    String.join(",", createColumnQueries)
            );
        });
        phrasesMap.put(Phrase.ADD_COLUMN, query -> {
            Column column = query.getColumn();

            boolean isAlterTable = query.getPhrases()
                    .get(0)
                    .equals(Phrase.ALTER_TABLE);

            return (isAlterTable ? "ADD COLUMN " : "") +
                    column.getColumnName() + " " +
                    getNativeColumnDefinition(column) + " " +
                    getDefaultValue(column) + " " +
                    (column.getIsNotNull().orElse(false) ? "NOT NULL" : "") + " " +
                    (column.getPrimary().orElse(false) ? "PRIMARY KEY" : "") + " ";
        });
    }

    protected String buildQuery(Query query, Function<Query, String>... queryBuilders) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Function<Query, String> queryBuilder : queryBuilders) {
            stringBuilder.append(queryBuilder.apply(query) + " ");
        }

        return stringBuilder.toString();
    }

    protected String getWithSizeIfPresent(Column column) {
        if (column.getSize().isPresent()) {
            return "(" + column.getSize().get() + ")";
        }

        return "";
    }

    protected String getWithSizeOrDefault(Column column, int defaultSize) {
        return "(" + column.getSize().orElse(defaultSize) + ")";
    }

    @Override
    public void alterColumnDefault(Table table, Column column) {
        Query query = new Query()
                .query(ALTER_TABLE, ALTER_COLUMN, SET_DEFAULT)
                .setTable(table)
                .setColumn(column);

        executeQuery(query);
    }

    protected void executeQuery(Query query) {
        try {
            executeInStatement(getQueries(query));
        } catch (SQLException e) {
            throw new CouldNotProcessException(e);
        }
    }

    protected String[] getQueries(Query query) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Phrase phrase : query.getPhrases()) {
            stringBuilder.append(translatePhrase(phrase).apply(query) + " ");
        }

        System.out.println(stringBuilder.toString());

        return new String[]{stringBuilder.toString()};
    }

    protected Function<Query, String> translatePhrase(Phrase phrase) {
        return phrasesMap.get(phrase);
    }

    public void alterColumnName(Table table, Column column) {
        Query query = new Query()
                .query(ALTER_TABLE, ALTER_COLUMN, RENAME)
                .setTable(table)
                .setColumn(column);

        executeQuery(query);
    }

    @Override
    public void alterColumnType(Table table, Column column) {
        Query query = new Query()
                .query(ALTER_TABLE, ALTER_COLUMN, TYPE)
                .setTable(table)
                .setColumn(column);

        executeQuery(query);
    }

    @Override
    public void addColumnsWithCreateTable(Table table) {
        if (table.getNewColumns().size() > 0) {
            Query query = new Query().query(CREATE_TABLE)
                    .setTable(table);

            executeQuery(query);
        }
    }

    @Override
    public void addColumnsWithAlterTable(Table table) {
        for (Column column : table.getNewColumns()) {
            Query query = new Query()
                    .query(ALTER_TABLE, ADD_COLUMN)
                    .setTable(table)
                    .setColumn(column);

            executeQuery(query);
        }
    }

    @Override
    public void addForeignKey(Table table, ForeignKey foreignKey) {
        Query query = new Query()
                .query(ALTER_TABLE, ADD_FOREIGN_KEY)
                .setTable(table)
                .setForeignKey(foreignKey);

        executeQuery(query);
    }

    @Override
    public void dropColumn(Table table, String columnName) {
        Query query = new Query()
                .query(ALTER_TABLE, DROP_COLUMN)
                .setTable(table)
                .setColumnName(columnName);

        executeQuery(query);
    }

    @Override
    public void dropForeignKey(Table table, String constraintName) {
        Query query = new Query()
                .query(ALTER_TABLE, DROP_FOREIGN_KEY)
                .setTable(table)
                .setConstraintName(constraintName);

        executeQuery(query);
    }

    @Override
    public void addConstraint(Table table, Constraint constraint) {
        Query query = new Query()
                .query(ALTER_TABLE, ADD_CONSTRAINT)
                .setTable(table)
                .setConstraint(constraint);

        executeQuery(query);
    }

    @Override
    public void dropConstraint(Table table, String constraintName) {
        Query query = new Query()
                .query(ALTER_TABLE, DROP_CONSTRAINT)
                .setTable(table)
                .setConstraintName(constraintName);

        executeQuery(query);
    }

    protected String getNativeCascadeType(ForeignKey.CASCADE cascade) {
        switch (cascade) {
            case RESTRICT:
                return "RESTRICT";
            case SET_NULL:
                return "SET NULL";
            case SET_DEFAULT:
                return "SET DEFAULT";
            case NO_ACTION:
                return "NO ACTION";
            case CASCADE:
                return "CASCADE";
        }
        throw new RuntimeException("Unknown type");
    }

    protected String getNativeConstraintType(Constraint.TYPE type) {
        switch (type) {
            case INDEX:
                return "INDEX";
            case UNIQUE:
                return "UNIQUE";
        }

        throw new RuntimeException("Could not process native constraint type");
    }

    protected String getDefaultValue(Column column) {
        String quote = "";

        List<Column.TYPE> quotedTypes = Arrays.asList(
                Column.TYPE.CHAR,
                Column.TYPE.VARCHAR
        );
        if (quotedTypes.contains(column.getType().get())) {
            quote = "'";
        }

        return (column.getDefaultValue().isPresent() ? "DEFAULT " + quote + column.getDefaultValue().get() + quote + "" : "");
    }

    protected void executeInStatement(String query) throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute(query);
        statement.close();
    }

    protected void executeInStatement(String[] queries) throws SQLException {
        Statement statement = connection.createStatement();
        for (String query : queries) {
            statement.execute(query);
        }

        statement.close();
    }

    protected abstract String getNativeColumnDefinition(Column column);

    @Override
    public TableValidator getTableValidator() {
        return new TableValidator(connection);
    }
}