-- V4: refresh 토큰 저장 (D-037, B28)
-- 로그아웃하면 그 기기의 행을 지워 refresh 토큰을 무효화한다. 토큰 원문이 아니라 SHA-256 해시(16진수 64자)만 저장한다
-- 이 테이블이 생기기 전에 발급된 refresh 토큰은 행이 없어 거부된다(배포 후 한 번 다시 로그인)
CREATE TABLE `refresh_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint NOT NULL,
  `token_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
  KEY `idx_refresh_token_member` (`member_id`),
  CONSTRAINT `fk_refresh_token_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
