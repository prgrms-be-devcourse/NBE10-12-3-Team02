-- 운영 반영 전 중복 데이터가 없는지 먼저 확인한다.
SELECT social_provider, social_provider_id, COUNT(*)
FROM users
WHERE social_provider IS NOT NULL
  AND social_provider_id IS NOT NULL
GROUP BY social_provider, social_provider_id
HAVING COUNT(*) > 1;

-- MySQL 기준 신규 테이블 DDL
CREATE TABLE IF NOT EXISTS user_social_auth (
    social_auth_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(255) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    oauth_refresh_token TEXT NULL,
    create_date DATETIME(6) NULL,
    modify_date DATETIME(6) NULL,
    PRIMARY KEY (social_auth_id),
    CONSTRAINT fk_user_social_auth_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uk_user_social_auth_user
        UNIQUE (user_id),
    CONSTRAINT uk_user_social_auth_provider_account
        UNIQUE (provider, provider_id)
);

INSERT INTO user_social_auth (
    user_id,
    provider,
    provider_id,
    oauth_refresh_token,
    create_date,
    modify_date
)
SELECT
    user_id,
    social_provider,
    social_provider_id,
    oauth_refresh_token,
    create_date,
    modify_date
FROM users
WHERE social_provider IS NOT NULL
  AND social_provider_id IS NOT NULL;

-- 안정화 이후 별도 배포에서 수행:
-- ALTER TABLE users DROP INDEX uk_users_social_account;
-- ALTER TABLE users DROP COLUMN social_provider;
-- ALTER TABLE users DROP COLUMN social_provider_id;
-- ALTER TABLE users DROP COLUMN oauth_refresh_token;
