# Database compatibility

The framework supports selecting MySQL or PostgreSQL with `DB_TYPE`:

```text
DB_TYPE=mysql       # default
DB_TYPE=postgresql
```

Each module keeps database scripts under separate directories:

```text
src/main/resources/sql/mysql/
src/main/resources/sql/postgresql/
```

The `mysql` directory contains the original scripts. The `postgresql` directory
contains the first compatible version generated from those scripts. Before using
PostgreSQL in production, execute the scripts against a clean PostgreSQL schema
and fix any module-specific differences discovered by integration tests.

MyBatis mapper statements that differ by vendor use `databaseId="mysql"` and
`databaseId="postgresql"`. Standard `Page`/`IPage` pagination and `LIMIT/OFFSET`
queries do not need separate mapper versions.
