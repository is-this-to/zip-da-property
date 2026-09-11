CREATE TABLE `property_member_state`
(
    `member_id`       BIGINT       NOT NULL
        COMMENT 'Member 서비스가 발급한 외부 회원 ID',
    `withdrawn_at`    DATETIME(6)  NULL
        COMMENT 'MemberWithdrawn 이벤트 발생 UTC 시각',
    `sanction_scope`  VARCHAR(100) NULL
        COMMENT 'MemberSanctioned 이벤트의 제한 범위. 제재가 없으면 NULL',
    `sanctioned_at`   DATETIME(6)  NULL
        COMMENT '현재 제재 시작 UTC 시각. 제재가 없으면 NULL',
    `agent_id`        BIGINT       NULL
        COMMENT 'Member 서비스의 중개사 외부 ID',
    `agency_id`       BIGINT       NULL
        COMMENT 'Member 서비스의 중개사무소 외부 ID',
    `agent_active`    BOOLEAN      NOT NULL DEFAULT FALSE
        COMMENT 'AgentApproved 이후 true, AgentSuspended 이후 false',
    `last_event_id`   VARCHAR(100) NOT NULL
        COMMENT '마지막으로 상태에 반영한 Member eventId',
    `last_event_at`   DATETIME(6)  NOT NULL
        COMMENT '마지막으로 상태에 반영한 Member 이벤트 발생 UTC 시각',
    `created_at`      DATETIME(6)  NOT NULL
        COMMENT '읽기 모델 생성 UTC 시각',
    `updated_at`      DATETIME(6)  NOT NULL
        COMMENT '읽기 모델 마지막 갱신 UTC 시각',

    PRIMARY KEY (`member_id`),

    CONSTRAINT `chk_property_member_state_agent_pair`
        CHECK (
            (`agent_id` IS NULL AND `agency_id` IS NULL)
            OR
            (`agent_id` IS NOT NULL AND `agency_id` IS NOT NULL)
        ),

    INDEX `idx_property_member_state_agent`
        (`agent_id`, `agency_id`, `agent_active`),
    INDEX `idx_property_member_state_sanction`
        (`sanctioned_at`, `sanction_scope`)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = 'Property 서비스가 사용하는 Member 상태·중개 권한 읽기 모델';


CREATE TABLE `property_member_event_consumption`
(
    `event_id`      VARCHAR(100) NOT NULL
        COMMENT 'Member 이벤트 고유 ID. 중복 소비 방지 키',
    `event_type`    VARCHAR(50)  NOT NULL
        COMMENT 'MemberWithdrawn, MemberSanctioned, MemberSanctionReleased, AgentApproved, AgentSuspended',
    `member_id`     BIGINT       NOT NULL
        COMMENT '이벤트 대상 Member 외부 ID',
    `occurred_at`   DATETIME(6)  NOT NULL
        COMMENT 'Member 이벤트 발생 UTC 시각',
    `processed_at`  DATETIME(6)  NOT NULL
        COMMENT 'Property 서비스 처리 완료 UTC 시각',

    PRIMARY KEY (`event_id`),
    INDEX `idx_member_event_consumption_member_timeline`
        (`member_id`, `occurred_at`),
    INDEX `idx_member_event_consumption_type_timeline`
        (`event_type`, `processed_at`)
)
ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
COMMENT = 'Member 이벤트 eventId 기반 멱등 소비 기록';
