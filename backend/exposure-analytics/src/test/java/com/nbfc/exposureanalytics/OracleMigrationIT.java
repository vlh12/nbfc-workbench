package com.nbfc.exposureanalytics;
import java.sql.DriverManager;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import static org.assertj.core.api.Assertions.*;
@Testcontainers
class OracleMigrationIT {
    @Container static GenericContainer<?> oracle=new GenericContainer<>(DockerImageName.parse("container-registry.oracle.com/database/free:latest"))
        .withEnv("ORACLE_PWD","SyntheticAdmin26")
        .withExposedPorts(1521)
        .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*\\n",1).withStartupTimeout(Duration.ofMinutes(10)));
    @Test void migrationsRunAgainstOracle26aiAndPreserveDecimals() throws Exception {
        String url="jdbc:oracle:thin:@"+oracle.getHost()+":"+oracle.getMappedPort(1521)+"/FREEPDB1";
        try(var connection=DriverManager.getConnection(url,"system","SyntheticAdmin26"); var statement=connection.createStatement()) {
            statement.execute("CREATE USER SYN_TEST IDENTIFIED BY SyntheticTest26 QUOTA 100M ON USERS");
            statement.execute("GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO SYN_TEST");
        }
        try(var connection=DriverManager.getConnection(url,"SYN_TEST","SyntheticTest26"); var statement=connection.createStatement()) {
            statement.execute("CREATE TABLE synthetic_existing_data (id NUMBER PRIMARY KEY)");
            statement.executeUpdate("INSERT INTO synthetic_existing_data(id) VALUES (1)");
        }
        var flyway=Flyway.configure().dataSource(url,"SYN_TEST","SyntheticTest26")
            .table("exposure_analytics_flyway_history").baselineOnMigrate(true).baselineVersion("0")
            .locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try(var connection=DriverManager.getConnection(url,"SYN_TEST","SyntheticTest26"); var statement=connection.createStatement()) {
            try(var preserved=statement.executeQuery("SELECT COUNT(*) FROM synthetic_existing_data")) {
                assertThat(preserved.next()).isTrue();
                assertThat(preserved.getInt(1)).isEqualTo(1);
            }
            statement.executeUpdate("INSERT INTO exposure_record(id,batch_id,bank_id,nbfc_id,exposure_amount) VALUES ('synthetic-row','synthetic-batch','SYN-BANK-1','SYN-NBFC-1',10.10)");
            try(var result=statement.executeQuery("SELECT SUM(exposure_amount) FROM exposure_record")) {
                assertThat(result.next()).isTrue(); assertThat(result.getBigDecimal(1)).isEqualByComparingTo("10.10");
            }
        }
    }
}
