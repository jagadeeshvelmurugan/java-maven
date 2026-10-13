# Equity Composition Service — PostgreSQL 15 (no Lombok)

Context → CompositionRecord → MarketData → LinkedHashMap
serialised as `{"meta":{…},"content":[{…},…]}`, split into two equal BYTEA chunks.

## Build
    mvn clean package -DskipTests

## Start PostgreSQL 15 + pgAdmin
    docker compose up -d postgres pgadmin

## Run
    java -Xmx2g -jar target/equity-composition-service-1.0.0.jar
