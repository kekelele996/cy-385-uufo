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

-- 辅食喂养餐次：一餐记录多个食材
CREATE TABLE IF NOT EXISTS feeding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  meal_type VARCHAR(20) NOT NULL COMMENT 'BREAKFAST/LUNCH/DINNER/SNACK',
  eaten_at DATETIME NOT NULL,
  note VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_feeding_baby_time (baby_id, eaten_at)
) COMMENT='辅食喂养记录';

CREATE TABLE IF NOT EXISTS feeding_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feeding_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  UNIQUE KEY uk_feeding_ingredient (feeding_id, name),
  INDEX idx_fi_name (name)
) COMMENT='餐次食材明细';

-- 餐后反应：同一餐只允许一条有效（未撤销）反应，active_feeding_id 为生成列，撤销后置 NULL 以保留审计记录
CREATE TABLE IF NOT EXISTS meal_reaction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  feeding_id BIGINT NOT NULL,
  baby_id BIGINT NOT NULL,
  reaction_type VARCHAR(20) NOT NULL COMMENT 'POSITIVE 有过敏反应 / NEGATIVE 无异常',
  observed_at DATETIME NOT NULL COMMENT '观察登记时间，须在餐后72小时内',
  symptoms VARCHAR(255),
  note VARCHAR(255),
  revoked TINYINT NOT NULL DEFAULT 0,
  revoked_reason VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  revoked_at DATETIME NULL,
  active_feeding_id BIGINT GENERATED ALWAYS AS (CASE WHEN revoked = 0 THEN feeding_id ELSE NULL END) STORED,
  UNIQUE KEY uk_reaction_active_feeding (active_feeding_id),
  INDEX idx_mr_baby (baby_id, reaction_type, revoked)
) COMMENT='餐后72小时反应登记';

CREATE TABLE IF NOT EXISTS reaction_ingredient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reaction_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  implicated TINYINT NOT NULL DEFAULT 1 COMMENT 'POSITIVE 时为是否被牵连排除；NEGATIVE 时恒为0表示本次安全验证',
  UNIQUE KEY uk_reaction_ingredient (reaction_id, ingredient_name),
  INDEX idx_ri_reaction (reaction_id),
  INDEX idx_ri_name (ingredient_name)
) COMMENT='反应牵连/验证的食材快照';

-- 每个宝宝每个食材一条当前状态，作为排除/安全的唯一事实来源
CREATE TABLE IF NOT EXISTS ingredient_status (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  baby_id BIGINT NOT NULL,
  ingredient_name VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL COMMENT 'UNVERIFIED/SAFE/EXCLUDED',
  reason VARCHAR(500) NOT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_baby_ingredient (baby_id, ingredient_name)
) COMMENT='食材过敏排除状态';

INSERT IGNORE INTO food_recipe (id, month_age_min, month_age_max, name, ingredients, steps, nutrition, allergens) VALUES
(1, 6, 8, '高铁米粉', '大米、强化铁米粉', '米粉用60度温水冲调，单向搅拌成糊状', '富含碳水化合物与铁', ''),
(2, 6, 12, '南瓜米糊', '大米、南瓜', '南瓜蒸熟压泥，拌入冲好的米粉', '胡萝卜素、膳食纤维', ''),
(3, 6, 12, '苹果泥', '苹果', '苹果去皮去核，蒸8分钟后刮泥', '果胶、维生素C', ''),
(4, 6, 18, '香蕉牛油果泥', '香蕉、牛油果', '取果肉用勺压泥混合即可', '钾、优质脂肪', ''),
(5, 7, 18, '鳕鱼土豆泥', '鳕鱼、土豆', '鳕鱼去刺蒸熟，土豆蒸熟压泥，混合拌细', 'DHA、蛋白质', '鳕鱼'),
(6, 8, 18, '蛋黄米糊', '鸡蛋黄、大米', '鸡蛋煮熟取黄压泥，拌入米糊', '卵磷脂、铁', '鸡蛋'),
(7, 8, 24, '菠菜猪肝泥', '菠菜、猪肝', '猪肝去筋煮熟剁碎，菠菜焯水去草酸后切末混合', '铁、维生素A', ''),
(8, 8, 24, '核桃米糊', '大米、核桃', '核桃烤熟磨粉，取少量拌入米糊', '不饱和脂肪酸', '坚果'),
(9, 9, 30, '豆腐鸡蛋羹', '豆腐、鸡蛋', '内酯豆腐铺底，淋打散蛋液，蒸8分钟', '优质蛋白、钙', '大豆、鸡蛋'),
(10, 10, 36, '虾仁蔬菜粥', '大米、虾仁、胡萝卜、西兰花', '米粥煮软，下腌制虾仁碎与蔬菜丁同煮10分钟', '蛋白质、维生素', '虾'),
(11, 10, 36, '番茄牛肉面', '小麦面条、牛肉、番茄', '牛肉剁碎，番茄去皮炒出汁，加水煮软碎面', '铁、锌、碳水', '小麦'),
(12, 12, 36, '花生酱燕麦粥', '燕麦、花生酱', '燕麦煮软，加入无添加花生酱搅匀', '维生素B族、优质脂肪', '花生'),
(13, 12, 36, '三文鱼蔬菜饭团', '三文鱼、大米、胡萝卜', '三文鱼煎熟去刺捏碎，与软饭、胡萝卜末捏成小团', 'DHA、蛋白质', '鱼类'),
(14, 12, 36, '牛奶燕麦布丁', '牛奶、燕麦、鸡蛋', '燕麦泡软，与牛奶蛋液混合蒸12分钟', '钙、蛋白质', '牛奶、鸡蛋'),
(15, 12, 36, '猕猴桃酸奶杯', '酸奶、猕猴桃', '猕猴桃切丁铺底，倒入无糖酸奶', '益生菌、维生素C', '牛奶'),
(16, 12, 36, '芒果鸡肉饭', '大米、鸡肉、芒果', '鸡肉炖软撕丝，芒果切丁，配软饭同食', '蛋白质、胡萝卜素', '芒果');
