package com.zipdaproperty.domain.property.concurrency;

import com.zipdaproperty.domain.property.command.PropertyCreateCommand;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.domain.property.constant.PublisherType;
import com.zipdaproperty.domain.property.constant.TransactionStatus;
import com.zipdaproperty.domain.property.constant.TransactionType;
import com.zipdaproperty.domain.property.entity.Property;
import com.zipdaproperty.domain.property.idempotency.entity.PropertyIdempotency;
import com.zipdaproperty.domain.region.entity.Region;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "PROPERTY_CONCURRENCY_TEST_DB_NAME",
        matches = ".+_test"
)
@SpringBootTest(
        classes = PropertyConcurrencyMySqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.datasource.hikari.maximum-pool-size=6"
        }
)
class PropertyConcurrencyMySqlIntegrationTest {

    private static final String TEST_DB_NAME =
            System.getenv("PROPERTY_CONCURRENCY_TEST_DB_NAME");
    private static final AtomicBoolean DATABASE_INITIALIZED =
            new AtomicBoolean();
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        initializeTestDatabase();
        registry.add("spring.datasource.url", () -> databaseUrl(TEST_DB_NAME));
        registry.add("spring.datasource.username", () -> requiredEnv("DB_USER"));
        registry.add("spring.datasource.password", () -> requiredEnv("DB_PASSWORD"));
        registry.add(
                "spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver"
        );
    }

    @BeforeEach
    void clearTestRows() {
        executeTransaction(entityManager -> {
            entityManager.createQuery(
                    "delete from PropertyIdempotency"
            ).executeUpdate();
            entityManager.createQuery(
                    "delete from Property"
            ).executeUpdate();
        });
    }

    @AfterAll
    static void removeTestDatabase() {
        if (!DATABASE_INITIALIZED.get()) {
            return;
        }

        try (
                Connection connection = DriverManager.getConnection(
                        serverUrl(),
                        requiredEnv("DB_USER"),
                        requiredEnv("DB_PASSWORD")
                );
                Statement statement = connection.createStatement()
        ) {
            statement.execute("DROP DATABASE `" + TEST_DB_NAME + "`");
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "동시성 테스트 DB를 제거하지 못했습니다.",
                    exception
            );
        }
    }

    @Test
    void concurrentIdempotencyRegistration_allowsOneActiveRecord() {
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<AttemptResult> results = runConcurrently(
                () -> insertIdempotencyRecord(barrier, "first"),
                () -> insertIdempotencyRecord(barrier, "second")
        );

        assertThat(results).filteredOn(AttemptResult::success).hasSize(1);
        assertThat(results).filteredOn(result -> !result.success()).hasSize(1);
        assertThat(countRows("PropertyIdempotency")).isEqualTo(1L);
    }

    @Test
    void concurrentPropertyUpdate_rejectsOneStaleVersion() {
        Long propertyId = 9_120_000_000_001L;
        executeTransaction(entityManager -> entityManager.persist(
                Property.create(
                        propertyId,
                        createCommand(),
                        memberActor("setup")
                )
        ));
        CyclicBarrier barrier = new CyclicBarrier(2);

        List<AttemptResult> results = runConcurrently(
                () -> updateTransactionStatus(
                        propertyId,
                        TransactionStatus.RESERVED,
                        barrier,
                        "reserved"
                ),
                () -> updateTransactionStatus(
                        propertyId,
                        TransactionStatus.COMPLETED,
                        barrier,
                        "completed"
                )
        );

        assertThat(results).filteredOn(AttemptResult::success).hasSize(1);
        AttemptResult rejected = results.stream()
                .filter(result -> !result.success())
                .findFirst()
                .orElseThrow();
        assertThat(hasCause(rejected.failure(), OptimisticLockException.class))
                .isTrue();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            Property savedProperty = entityManager.find(Property.class, propertyId);
            assertThat(savedProperty.getVersion()).isEqualTo(1L);
            assertThat(savedProperty.getTransactionStatus()).isIn(
                    TransactionStatus.RESERVED,
                    TransactionStatus.COMPLETED
            );
        } finally {
            entityManager.close();
        }
    }

    private AttemptResult insertIdempotencyRecord(
            CyclicBarrier barrier,
            String traceSuffix
    ) {
        return executeAttempt(entityManager -> {
            await(barrier);
            entityManager.persist(PropertyIdempotency.start(
                    1001L,
                    "POST:/api/property/properties",
                    "concurrent-idempotency-key",
                    "a".repeat(64),
                    Instant.now().plus(Duration.ofHours(24)),
                    memberActor(traceSuffix)
            ));
        });
    }

    private AttemptResult updateTransactionStatus(
            Long propertyId,
            TransactionStatus targetStatus,
            CyclicBarrier barrier,
            String traceSuffix
    ) {
        return executeAttempt(entityManager -> {
            Property property = entityManager.find(Property.class, propertyId);
            assertThat(property.getVersion()).isZero();
            await(barrier);
            property.changeTransactionStatus(
                    targetStatus,
                    memberActor(traceSuffix)
            );
        });
    }

    private List<AttemptResult> runConcurrently(
            Attempt firstAttempt,
            Attempt secondAttempt
    ) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<AttemptResult> first = executorService.submit(firstAttempt::run);
            Future<AttemptResult> second = executorService.submit(secondAttempt::run);

            return List.of(
                    first.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                    second.get(WAIT_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "동시성 테스트 실행을 완료하지 못했습니다.",
                    exception
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    private AttemptResult executeAttempt(EntityManagerWork work) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            work.execute(entityManager);
            entityManager.flush();
            entityManager.getTransaction().commit();
            return AttemptResult.succeeded();
        } catch (RuntimeException exception) {
            rollback(entityManager);
            return AttemptResult.failed(exception);
        } finally {
            entityManager.close();
        }
    }

    private void executeTransaction(EntityManagerWork work) {
        AttemptResult result = executeAttempt(work);
        if (!result.success()) {
            throw new IllegalStateException(
                    "동시성 테스트 데이터 준비에 실패했습니다.",
                    result.failure()
            );
        }
    }

    private long countRows(String entityName) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return entityManager.createQuery(
                    "select count(entity) from " + entityName + " entity",
                    Long.class
            ).getSingleResult();
        } finally {
            entityManager.close();
        }
    }

    private static void rollback(EntityManager entityManager) {
        if (entityManager.getTransaction().isActive()) {
            entityManager.getTransaction().rollback();
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(
                    WAIT_TIMEOUT.toSeconds(),
                    TimeUnit.SECONDS
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "동시 요청 시작점을 맞추지 못했습니다.",
                    exception
            );
        }
    }

    private static boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> causeType
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static PropertyCreateCommand createCommand() {
        return new PropertyCreateCommand(
                1L,
                null,
                PublisherType.DIRECT_OWNER,
                PropertyType.APARTMENT,
                TransactionType.SALE,
                500_000_000L,
                null,
                null,
                150_000L,
                new BigDecimal("84.99"),
                new BigDecimal("59.99"),
                3,
                1,
                5,
                20,
                "중층",
                "남향",
                null,
                "공동주택",
                true,
                true,
                false,
                "동시성 통합 테스트 매물",
                "낙관적 잠금 동시성 검증용 매물입니다.",
                null,
                null,
                null
        );
    }

    private static ActorContext memberActor(String traceSuffix) {
        return ActorContext.member(
                1001L,
                ActorRole.USER,
                "property-concurrency-test-" + traceSuffix
        );
    }

    private static synchronized void initializeTestDatabase() {
        if (DATABASE_INITIALIZED.get()) {
            return;
        }

        validateTestDatabaseName();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (
                    Connection connection = DriverManager.getConnection(
                            serverUrl(),
                            requiredEnv("DB_USER"),
                            requiredEnv("DB_PASSWORD")
                    );
                    Statement statement = connection.createStatement()
            ) {
                statement.execute(
                        "DROP DATABASE IF EXISTS `" + TEST_DB_NAME + "`"
                );
                statement.execute(
                        "CREATE DATABASE `" + TEST_DB_NAME
                                + "` CHARACTER SET utf8mb4"
                                + " COLLATE utf8mb4_0900_ai_ci"
                );
            }

            executeSchemaFile("database/schema/001_create_property_core_tables.sql");
            executeSchemaFile("database/schema/004_create_property_idempotency_table.sql");
            executeSchemaFile("database/schema/014_add_property_idempotency_cleanup.sql");
            DATABASE_INITIALIZED.set(true);
        } catch (ClassNotFoundException | SQLException | IOException exception) {
            throw new IllegalStateException(
                    "동시성 테스트 DB를 초기화하지 못했습니다.",
                    exception
            );
        }
    }

    private static void executeSchemaFile(String filePath)
            throws SQLException, IOException {
        String sql = Files.readString(
                Path.of(filePath),
                StandardCharsets.UTF_8
        );
        try (
                Connection connection = DriverManager.getConnection(
                        databaseUrl(TEST_DB_NAME),
                        requiredEnv("DB_USER"),
                        requiredEnv("DB_PASSWORD")
                );
                Statement statement = connection.createStatement()
        ) {
            statement.execute(sql);
        }
    }

    private static String serverUrl() {
        return "jdbc:mysql://"
                + requiredEnv("DB_HOST")
                + ":"
                + requiredEnv("DB_PORT")
                + "/?allowMultiQueries=true&allowPublicKeyRetrieval=true"
                + "&useSSL=false&serverTimezone=UTC";
    }

    private static String databaseUrl(String databaseName) {
        return "jdbc:mysql://"
                + requiredEnv("DB_HOST")
                + ":"
                + requiredEnv("DB_PORT")
                + "/"
                + databaseName
                + "?allowMultiQueries=true&allowPublicKeyRetrieval=true"
                + "&useSSL=false&serverTimezone=UTC";
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    name + " 환경변수는 동시성 통합 테스트에 필수입니다."
            );
        }
        return value;
    }

    private static void validateTestDatabaseName() {
        if (
                TEST_DB_NAME == null
                        || !TEST_DB_NAME.matches("[A-Za-z0-9_]+_test")
        ) {
            throw new IllegalStateException(
                    "PROPERTY_CONCURRENCY_TEST_DB_NAME은 _test로 끝나는"
                            + " 안전한 DB 이름이어야 합니다."
            );
        }
    }

    @FunctionalInterface
    private interface Attempt {
        AttemptResult run();
    }

    @FunctionalInterface
    private interface EntityManagerWork {
        void execute(EntityManager entityManager);
    }

    private record AttemptResult(
            boolean success,
            Throwable failure
    ) {
        private static AttemptResult succeeded() {
            return new AttemptResult(true, null);
        }

        private static AttemptResult failed(Throwable failure) {
            return new AttemptResult(false, failure);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaAuditing
    @EntityScan(basePackageClasses = {
            Property.class,
            PropertyIdempotency.class,
            Region.class
    })
    static class TestApplication {
    }
}
