package com.zipdaproperty.domain.report.service;

import com.zipdaproperty.domain.file.constant.FilePurpose;
import com.zipdaproperty.domain.file.constant.UploadStatus;
import com.zipdaproperty.domain.file.entity.PropertyFile;
import com.zipdaproperty.domain.file.repository.PropertyFileRepository;
import com.zipdaproperty.domain.image.entity.PropertyImage;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRepository;
import com.zipdaproperty.domain.property.repository.PropertyPublicDetailQueryRow;
import com.zipdaproperty.domain.report.entity.PropertyReport;
import com.zipdaproperty.domain.report.entity.PropertyReportAction;
import com.zipdaproperty.domain.report.entity.PropertyReportAppeal;
import com.zipdaproperty.domain.report.entity.PropertyReportEvidence;
import com.zipdaproperty.domain.report.repository.PropertyReportEvidenceRepository;
import com.zipdaproperty.domain.report.repository.PropertyReportRepository;
import com.zipdaproperty.domain.report.type.ReportReasonCode;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.context.constant.ActorRole;
import com.zipdaproperty.global.id.TsidGenerator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(
        named = "PROPERTY_REPORT_TEST_DB_NAME",
        matches = ".+_test"
)
@SpringBootTest(
        classes = PropertyReportMySqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.sql.init.mode=never",
                "spring.datasource.hikari.maximum-pool-size=6"
        }
)
class PropertyReportMySqlIntegrationTest {

    private static final String TEST_DB_NAME =
            System.getenv("PROPERTY_REPORT_TEST_DB_NAME");
    private static final AtomicBoolean DATABASE_INITIALIZED =
            new AtomicBoolean();
    private static final Long REPORTER_MEMBER_ID = 1001L;
    private static final Long EVIDENCE_FILE_ID = 884700000000000003L;

    @Autowired
    private PropertyReportRepository reportRepository;

    @Autowired
    private PropertyReportEvidenceRepository evidenceRepository;

    @Autowired
    private PropertyFileRepository fileRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        jdbcTemplate.update("DELETE FROM property_report_evidence");
        jdbcTemplate.update("DELETE FROM property_report");
        jdbcTemplate.update("DELETE FROM property_file");
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
                    "신고 통합 테스트 DB를 제거하지 못했습니다.",
                    exception
            );
        }
    }

    @Test
    void evidenceUniqueFailure_rollsBackReportAndFileLink() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);
        ActorContext actorContext = memberActor("rollback-setup");
        Long reportId = 884700000000000300L;
        transactionTemplate.executeWithoutResult(status -> {
            PropertyFile file = PropertyFile.create(
                    EVIDENCE_FILE_ID,
                    "report-evidence-integration",
                    FilePurpose.REPORT_EVIDENCE,
                    "evidence.jpg",
                    1024L,
                    "report/evidence/integration.jpg",
                    Instant.now().plus(Duration.ofHours(1)),
                    actorContext
            );
            file.complete(
                    "a".repeat(64),
                    "image/jpeg",
                    actorContext
            );
            fileRepository.saveAndFlush(file);
        });
        jdbcTemplate.update(
                """
                        INSERT INTO property_report_evidence (
                            report_id,
                            property_file_id,
                            evidence_type,
                            sort_order,
                            created_at,
                            created_by_member_id,
                            created_by_role,
                            updated_at,
                            updated_by_member_id,
                            updated_by_role,
                            action_source
                        ) VALUES (?, ?, 'SCREENSHOT', 0, NOW(6), ?, 'USER',
                                  NOW(6), ?, 'USER', 'MEMBER')
                        """,
                reportId,
                EVIDENCE_FILE_ID,
                REPORTER_MEMBER_ID,
                REPORTER_MEMBER_ID
        );
        PropertyReportService service = serviceWithFixedId(reportId);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
                status -> service.createReport(
                        4001L,
                        ReportReasonCode.FALSE_INFO,
                        "증빙 저장 실패 롤백을 검증하는 신고 상세입니다.",
                        List.of(EVIDENCE_FILE_ID),
                        memberActor("rollback-attempt")
                )
        )).isInstanceOf(RuntimeException.class);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_report WHERE report_id = ?",
                Long.class,
                reportId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_report_evidence WHERE report_id = ?",
                Long.class,
                reportId
        )).isEqualTo(1L);
        assertThat(fileRepository.findById(EVIDENCE_FILE_ID))
                .get()
                .extracting(PropertyFile::getUploadStatus)
                .isEqualTo(UploadStatus.VERIFIED);
    }

    private PropertyReportService serviceWithFixedId(Long reportId) {
        TsidGenerator generator = mock(TsidGenerator.class);
        when(generator.generate()).thenReturn(reportId);
        return service(generator);
    }

    private PropertyReportService service(TsidGenerator generator) {
        PropertyPublicDetailQueryRepository publicDetailQueryRepository =
                mock(PropertyPublicDetailQueryRepository.class);
        when(publicDetailQueryRepository.findPublicDetail(anyLong()))
                .thenReturn(Optional.of(mock(PropertyPublicDetailQueryRow.class)));
        return new PropertyReportService(
                reportRepository,
                publicDetailQueryRepository,
                evidenceRepository,
                fileRepository,
                generator
        );
    }

    private static ActorContext memberActor(String traceSuffix) {
        return ActorContext.member(
                REPORTER_MEMBER_ID,
                ActorRole.USER,
                "property-report-integration-" + traceSuffix
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
                statement.execute("DROP DATABASE IF EXISTS `" + TEST_DB_NAME + "`");
                statement.execute(
                        "CREATE DATABASE `" + TEST_DB_NAME
                                + "` CHARACTER SET utf8mb4"
                                + " COLLATE utf8mb4_0900_ai_ci"
                );
            }

            executeSchemaFile("database/schema/005_create_property_file_table.sql");
            executeSchemaFile("database/schema/005_create_property_report_table.sql");
            executeSchemaFile("database/schema/018_create_property_report_evidence_table.sql");
            DATABASE_INITIALIZED.set(true);
        } catch (ClassNotFoundException | SQLException | IOException exception) {
            throw new IllegalStateException(
                    "신고 통합 테스트 DB를 초기화하지 못했습니다.",
                    exception
            );
        }
    }

    private static void executeSchemaFile(String filePath)
            throws SQLException, IOException {
        String sql = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
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
                    name + " 환경변수는 신고 통합 테스트에 필수입니다."
            );
        }
        return value;
    }

    private static void validateTestDatabaseName() {
        if (TEST_DB_NAME == null || !TEST_DB_NAME.matches("[A-Za-z0-9_]+_test")) {
            throw new IllegalStateException(
                    "PROPERTY_REPORT_TEST_DB_NAME은 _test로 끝나는 안전한 DB 이름이어야 합니다."
            );
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaAuditing
    @EnableJpaRepositories(basePackageClasses = {
            PropertyReportRepository.class,
            PropertyFileRepository.class
    })
    @EntityScan(basePackageClasses = {
            PropertyReport.class,
            PropertyReportEvidence.class,
            PropertyReportAction.class,
            PropertyReportAppeal.class,
            PropertyFile.class,
            PropertyImage.class
    })
    static class TestApplication {
    }
}
