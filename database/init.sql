CREATE TABLE IF NOT EXISTS baby (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  birthday DATE NOT NULL,
  blood_type VARCHAR(10),
  initial_height DECIMAL(5,2),
  initial_weight DECIMAL(5,2)
);

CREATE TABLE IF NOT EXISTS growth_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  recorded_at DATE NOT NULL,
  height_cm DECIMAL(5,2),
  weight_kg DECIMAL(5,2),
  percentile VARCHAR(40)
);

CREATE TABLE IF NOT EXISTS vaccine_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  vaccine_name VARCHAR(120) NOT NULL,
  planned_date DATE NOT NULL,
  completed BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS food_recipe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  month_age_min INT NOT NULL,
  month_age_max INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  ingredients TEXT,
  steps TEXT,
  nutrition TEXT,
  allergens VARCHAR(160)
);

-- 辅食喂养记录：一餐包含多个食材
CREATE TABLE IF NOT EXISTS feeding_meal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  meal_type VARCHAR(16) NOT NULL DEFAULT '辅食',
  meal_time DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_meal_baby_time (baby_id, meal_time)
);

CREATE TABLE IF NOT EXISTS feeding_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  meal_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  KEY idx_ingredient_meal (meal_id)
);

-- 餐后反应登记：一餐至多一条“有效（未撤销）”反应。
-- active_meal_id 为生成列：未撤销时等于 meal_id，撤销后为 NULL；
-- 依赖唯一索引在数据库层保证重复/并发提交只产生一条有效反应（MySQL 唯一索引允许多个 NULL）。
CREATE TABLE IF NOT EXISTS allergy_reaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  meal_id BIGINT NOT NULL,
  reacted BOOLEAN NOT NULL COMMENT 'TRUE=出现过敏反应；FALSE=餐后满72小时无反应',
  symptoms VARCHAR(255),
  reacted_at DATETIME(3) NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  revoked_at DATETIME(3),
  revoke_reason VARCHAR(255),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  active_meal_id BIGINT GENERATED ALWAYS AS (IF(revoked = 0, meal_id, NULL)) STORED,
  UNIQUE KEY uk_reaction_active_meal (active_meal_id),
  KEY idx_reaction_meal (meal_id)
);

-- 食材安全状态投影：SAFE=已确认安全；EXCLUDED=已排除。不存在的行表示尚未验证。
CREATE TABLE IF NOT EXISTS baby_ingredient_status (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  status VARCHAR(16) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  first_confirmed_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  UNIQUE KEY uk_status_baby_ingredient (baby_id, ingredient_name)
);

-- 状态变更流水：喂养记录、反应与排除状态一次生效后留痕，撤销误报时可追溯原因
CREATE TABLE IF NOT EXISTS ingredient_status_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  old_status VARCHAR(16),
  new_status VARCHAR(16),
  reason VARCHAR(500) NOT NULL,
  related_reaction_id BIGINT,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_log_baby (baby_id, ingredient_name)
);

INSERT IGNORE INTO food_recipe (id, month_age_min, month_age_max, name, ingredients, steps, nutrition, allergens) VALUES
  (1, 6, 12, '南瓜米糊', '南瓜、大米', '南瓜蒸熟压泥，拌入熬烂的米糊。', '碳水化合物、β-胡萝卜素', NULL),
  (2, 7, 18, '鳕鱼土豆泥', '鳕鱼、土豆', '鳕鱼去刺蒸熟，土豆蒸熟压泥后拌匀。', '优质蛋白、DHA', '鱼类'),
  (3, 6, 24, '苹果燕麦粥', '苹果、燕麦', '燕麦煮软，加入擦丝的苹果略煮。', '膳食纤维、铁', '麸质'),
  (4, 8, 24, '蛋黄米糊', '鸡蛋黄、大米', '熟蛋黄压碎拌入米糊，从四分之一个开始尝试。', '卵磷脂、铁', '鸡蛋'),
  (5, 8, 18, '菠菜豆腐泥', '菠菜、豆腐', '菠菜焯水去草酸，与嫩豆腐一同压泥。', '钙、铁、植物蛋白', '大豆'),
  (6, 6, 36, '香蕉牛油果泥', '香蕉、牛油果', '香蕉与牛油果果肉一同压成泥。', '钾、健康脂肪', NULL),
  (7, 10, 36, '番茄牛肉面', '番茄、牛肉、小麦面条', '牛肉末与番茄炖烂，拌入煮软的碎面条。', '铁、锌、蛋白质', '麸质'),
  (8, 9, 36, '胡萝卜虾仁粥', '胡萝卜、虾仁、大米', '虾仁剁碎，与胡萝卜丁、大米同煮成粥。', '钙、优质蛋白', '甲壳类');
