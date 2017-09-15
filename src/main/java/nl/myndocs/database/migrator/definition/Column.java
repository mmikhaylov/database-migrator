package nl.myndocs.database.migrator.definition;

import java.util.Optional;

/**
 * Created by albert on 13-8-2017.
 */
public class Column {
    public enum TYPE {
        INTEGER, CHAR, VARCHAR, UUID, DATE, TIME, TIMESTAMP
    }

    private String columnName;
    private Boolean primary;
    private Boolean autoIncrement;
    private Boolean isNotNull;
    private TYPE type;
    private Integer size;
    private String defaultValue;
    private String rename;

    private Column(Builder builder) {
        columnName = builder.getColumnName();
        primary = builder.getPrimary();
        autoIncrement = builder.getAutoIncrement();
        isNotNull = builder.getNotNull();
        type = builder.getType();
        size = builder.getSize();
        defaultValue = builder.getDefaultValue();
        rename = builder.getRename();
    }

    public String getColumnName() {
        return columnName;
    }

    public Optional<Boolean> getPrimary() {
        return Optional.ofNullable(primary);
    }

    public Optional<String> getRename() {
        return Optional.ofNullable(rename);
    }

    public Optional<Boolean> getAutoIncrement() {
        return Optional.ofNullable(autoIncrement);
    }

    public Optional<Boolean> getIsNotNull() {
        return Optional.ofNullable(isNotNull);
    }

    public Optional<TYPE> getType() {
        return Optional.ofNullable(type);
    }

    public Optional<Integer> getSize() {
        return Optional.ofNullable(size);
    }

    public Optional<String> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public static class Builder {
        private String columnName;
        private Boolean primary;
        private Boolean autoIncrement;
        private TYPE type;
        private Boolean notNull;
        private Integer size;
        private String defaultValue;
        private String rename;

        public Builder(String columnName, Column.TYPE type) {
            if (columnName == null || type == null) {
                throw new IllegalArgumentException("Parameter should not be null");
            }

            this.columnName = columnName;
            this.type = type;
        }

        public Builder(String columnName) {
            this.columnName = columnName;
        }

        public Builder rename(String name) {
            rename = name;

            return this;
        }

        public Builder type(Column.TYPE type) {
            this.type = type;

            return this;
        }

        public Builder primary(Boolean primary) {
            this.primary = primary;

            return this;
        }

        public Builder autoIncrement(Boolean autoIncrement) {
            this.autoIncrement = autoIncrement;

            return this;
        }

        public Builder defaultValue(String value) {
            this.defaultValue = value;

            return this;
        }

        public Builder size(Integer size) {
            this.size = size;

            return this;
        }

        public Builder notNull(Boolean notNull) {
            this.notNull = notNull;

            return this;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getRename() {
            return rename;
        }

        public Boolean getPrimary() {
            return primary;
        }

        public Boolean getAutoIncrement() {
            return autoIncrement;
        }

        public TYPE getType() {
            return type;
        }

        public Boolean getNotNull() {
            return notNull;
        }

        public Integer getSize() {
            return size;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public Column build() {
            return new Column(this);
        }
    }
}
