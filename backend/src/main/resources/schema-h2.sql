-- H2（MySQL 兼容模式）测试结构，语义与 database/init.sql 保持一致

CREATE TABLE baby (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  birthday DATE NOT NULL,
  blood_type VARCHAR(10),
  initial_height DECIMAL(5,2),
  initial_weight DECIMAL(5,2)
);

CREATE TABLE growth_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  recorded_at DATE NOT NULL,
  height_cm DECIMAL(5,2),
  weight_kg DECIMAL(5,2),
  percentile VARCHAR(40)
);

CREATE TABLE vaccine_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  vaccine_name VARCHAR(120) NOT NULL,
  planned_date DATE NOT NULL,
  completed BOOLEAN DEFAULT FALSE
);

CREATE TABLE food_recipe (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  month_age_min INT NOT NULL,
  month_age_max INT NOT NULL,
  name VARCHAR(120) NOT NULL,
  ingredients CLOB,
  steps CLOB,
  nutrition CLOB,
  allergens VARCHAR(160)
);

CREATE TABLE feeding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  eaten_at TIMESTAMP NOT NULL,
  note VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_feeding_baby_time ON feeding (baby_id, eaten_at);

CREATE TABLE feeding_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feeding_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  CONSTRAINT uk_feeding_ingredient UNIQUE (feeding_id, name)
);
CREATE INDEX idx_fi_name ON feeding_ingredient (name);

CREATE TABLE meal_reaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feeding_id BIGINT NOT NULL,
  baby_id BIGINT NOT NULL,
  reaction_type VARCHAR(20) NOT NULL,
  observed_at TIMESTAMP NOT NULL,
  symptoms VARCHAR(255),
  note VARCHAR(255),
  revoked TINYINT NOT NULL DEFAULT 0,
  revoked_reason VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP NULL,
  active_feeding_id BIGINT GENERATED ALWAYS AS (CASE WHEN revoked = 0 THEN feeding_id ELSE NULL END),
  CONSTRAINT uk_reaction_active_feeding UNIQUE (active_feeding_id)
);
CREATE INDEX idx_mr_baby ON meal_reaction (baby_id, reaction_type, revoked);

CREATE TABLE reaction_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reaction_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  implicated TINYINT NOT NULL DEFAULT 1,
  CONSTRAINT uk_reaction_ingredient UNIQUE (reaction_id, ingredient_name)
);
CREATE INDEX idx_ri_reaction ON reaction_ingredient (reaction_id);
CREATE INDEX idx_ri_name ON reaction_ingredient (ingredient_name);

CREATE TABLE ingredient_status (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_baby_ingredient UNIQUE (baby_id, ingredient_name)
);

INSERT INTO food_recipe (id, month_age_min, month_age_max, name, ingredients, steps, nutrition, allergens) VALUES
(1, 6, 8, '高铁米粉', '大米、强化铁米粉', '冲调', '铁', NULL),
(5, 7, 18, '鳕鱼土豆泥', '鳕鱼、土豆', '蒸熟压泥', 'DHA', '鳕鱼'),
(10, 10, 36, '虾仁蔬菜粥', '大米、虾仁、胡萝卜、西兰花', '同煮', '蛋白质', '虾');
