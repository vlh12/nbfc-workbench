package com.nbfc.accessdata;
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
            .table("access_data_flyway_history").baselineOnMigrate(true).baselineVersion("0")
            .locations("classpath:db/migration").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(8);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try(var factory=new org.hibernate.cfg.Configuration()
                .addAnnotatedClass(com.nbfc.accessdata.data.Batch.class)
                .addAnnotatedClass(com.nbfc.accessdata.auth.ApplicationUser.class)
                .setProperty("hibernate.connection.url",url)
                .setProperty("hibernate.connection.username","SYN_TEST")
                .setProperty("hibernate.connection.password","SyntheticTest26")
                .setProperty("hibernate.hbm2ddl.auto","validate")
                .buildSessionFactory()) {
            try(var session=factory.openSession()) {
                var transaction=session.beginTransaction();
                session.persist(new com.nbfc.accessdata.data.Batch("synthetic-mapping-batch","synthetic-subject",1));
                session.persist(new com.nbfc.accessdata.data.OutboxEvent("synthetic-mapping-event","synthetic-mapping-batch","{}"));
                transaction.commit();
            }
            try(var session=factory.openSession()) {
                var transaction=session.beginTransaction();
                var event=session.find(com.nbfc.accessdata.data.OutboxEvent.class,"synthetic-mapping-event");
                assertThat(event.published).isFalse();
                event.published=true;
                transaction.commit();
            }
            try(var connection=DriverManager.getConnection(url,"SYN_TEST","SyntheticTest26");
                    var statement=connection.createStatement();
                    var value=statement.executeQuery("SELECT published FROM outbox_event WHERE id='synthetic-mapping-event'")) {
                assertThat(value.next()).isTrue();
                assertThat(value.getInt(1)).isEqualTo(1);
            }
        }
        try(var connection=DriverManager.getConnection(url,"SYN_TEST","SyntheticTest26"); var statement=connection.createStatement()) {
            try(var preserved=statement.executeQuery("SELECT COUNT(*) FROM synthetic_existing_data")) {
                assertThat(preserved.next()).isTrue();
                assertThat(preserved.getInt(1)).isEqualTo(1);
            }
            statement.executeUpdate("INSERT INTO exposure_batch(id,submitted_by,row_count,status,created_at) VALUES ('synthetic-batch','synthetic-subject',1,'VALIDATED',CURRENT_TIMESTAMP)");
            statement.executeUpdate("INSERT INTO outbox_event(id,batch_id,payload,published,created_at) VALUES ('synthetic-event','synthetic-batch','{}',0,CURRENT_TIMESTAMP)");
            try(var result=statement.executeQuery("SELECT COUNT(*) FROM outbox_event WHERE published=0")) {
                assertThat(result.next()).isTrue(); assertThat(result.getInt(1)).isEqualTo(1);
            }
        }
    }
}
