-- V1: Flyway 도입 시점(2026-10-08)의 스키마 (D-023)
-- 당시 엔티티로 빈 DB에 Hibernate ddl-auto=create를 실행한 뒤 mysqldump --no-data로 만들었다. 기존 로컬 DB와 같은 것을 확인함
-- 이미 테이블이 있는 DB는 baseline-on-migrate로 이 파일을 적용된 것으로 보고 건너뛴다. 이 파일은 고치지 않는다

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `allergy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ingredient_id` bigint DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfxjhgc42wd3iy5maoso2b5mgr` (`ingredient_id`),
  KEY `FKc0dyjrcuxajv7btb318j1d7dq` (`member_id`),
  CONSTRAINT `FKc0dyjrcuxajv7btb318j1d7dq` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKfxjhgc42wd3iy5maoso2b5mgr` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `history` (
  `favorite` bit(1) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `recipe_id` bigint DEFAULT NULL,
  `view_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbnwj6i7md9xd8vr1pfqqbf1q5` (`member_id`),
  KEY `FKpq16bwx4o63lpnkche66u87x2` (`recipe_id`),
  CONSTRAINT `FKbnwj6i7md9xd8vr1pfqqbf1q5` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKpq16bwx4o63lpnkche66u87x2` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `ingredient` (
  `carbohydrate_g` double DEFAULT NULL,
  `category` tinyint DEFAULT NULL,
  `dietary_fiber_g` double DEFAULT NULL,
  `energy_kcal` double DEFAULT NULL,
  `fat_g` double DEFAULT NULL,
  `protein_g` double DEFAULT NULL,
  `sodium_mg` double DEFAULT NULL,
  `sugar_g` double DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_food_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nutrient_basis` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `ingredient_chk_1` CHECK ((`category` between 0 and 12))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_version` bigint NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `external_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `picture` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKmbmcqelty0fbrvxp1q58dn57t` (`email`),
  UNIQUE KEY `UKgunhfxpujivoghi8k7gbnc3o3` (`external_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recipe` (
  `carbohydrate` double DEFAULT NULL,
  `fat` double DEFAULT NULL,
  `is_used` bit(1) NOT NULL,
  `kcal` double DEFAULT NULL,
  `protein` double DEFAULT NULL,
  `sodium` double DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ingredients` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `portion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recipe_ingredient` (
  `quantity` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ingredient_id` bigint DEFAULT NULL,
  `recipe_id` bigint DEFAULT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9b3oxoskt0chwqxge0cnlkc29` (`ingredient_id`),
  KEY `FKgu1oxq7mbcgkx5dah6o8geirh` (`recipe_id`),
  CONSTRAINT `FK9b3oxoskt0chwqxge0cnlkc29` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient` (`id`),
  CONSTRAINT `FKgu1oxq7mbcgkx5dah6o8geirh` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `recipe_step` (
  `step_num` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipe_id` bigint DEFAULT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKodjhx0drj0jgeb0xnvl2g6oft` (`recipe_id`),
  CONSTRAINT `FKodjhx0drj0jgeb0xnvl2g6oft` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refrigerator_ingredient` (
  `category` tinyint DEFAULT NULL,
  `quantity` int NOT NULL,
  `type` tinyint DEFAULT NULL,
  `expire_date` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ingredient_id` bigint DEFAULT NULL,
  `input_date` datetime(6) DEFAULT NULL,
  `member_id` bigint DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKovbtl72c7mygc40p7eyjjna2w` (`ingredient_id`),
  KEY `FK4k6xqk1etmg2gfigewp0b9q` (`member_id`),
  CONSTRAINT `FK4k6xqk1etmg2gfigewp0b9q` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKovbtl72c7mygc40p7eyjjna2w` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient` (`id`),
  CONSTRAINT `refrigerator_ingredient_chk_1` CHECK ((`category` between 0 and 12)),
  CONSTRAINT `refrigerator_ingredient_chk_2` CHECK ((`type` between 0 and 2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `taste` (
  `type` tinyint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `member_id` bigint DEFAULT NULL,
  `recipe_id` bigint DEFAULT NULL,
  `portion` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKg32c56ww893cmgfvdauavo99g` (`member_id`),
  KEY `FKngd9n7shdgqp57t640yhwfgn4` (`recipe_id`),
  CONSTRAINT `FKg32c56ww893cmgfvdauavo99g` FOREIGN KEY (`member_id`) REFERENCES `member` (`id`),
  CONSTRAINT `FKngd9n7shdgqp57t640yhwfgn4` FOREIGN KEY (`recipe_id`) REFERENCES `recipe` (`id`),
  CONSTRAINT `taste_chk_1` CHECK ((`type` between 0 and 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
