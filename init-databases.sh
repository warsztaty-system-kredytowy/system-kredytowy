#!/bin/bash
set -e

recreate_db() {
    local db_name=$1
    echo "Recreating database: $db_name"

    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        -- disconnect all users
        SELECT pg_terminate_backend(pg_stat_activity.pid)
        FROM pg_stat_activity
        WHERE pg_stat_activity.datname = '$db_name'
            AND pid <> pg_backend_pid();
        
        DROP DATABASE IF EXISTS "$db_name";
        CREATE DATABASE "$db_name";
        GRANT ALL PRIVILEGES ON DATABASE "$db_name" TO "$POSTGRES_USER";
EOSQL
}

recreate_db "app_db"
recreate_db "keycloak_db"