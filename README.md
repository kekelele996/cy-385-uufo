# 宝宝成长记录与辅食管理平台

帮助新手父母管理宝宝档案、生长曲线、疫苗计划、辅食食谱和喂养记录，并提供**辅食过敏排除闭环**：多食材喂养记录 → 餐后 72 小时反应登记 → 食材安全/排除状态自动推导 → 食谱推荐自动屏蔽 → 误报可撤销恢复。

## 快速启动

```bash
cp .env.example .env
docker compose up -d --build
```

访问地址：前端 http://localhost:18405 ，后端 http://localhost:19405/health 。

## 项目主要功能

- 创建多个宝宝档案，记录出生日期、身高、体重和血型。
- 定期记录身高体重，自动生成成长曲线百分位。
- 内置疫苗计划，支持已接种和未接种状态。
- 按月龄推荐辅食食谱，并支持过敏原筛选。
- 记录每日喂养内容、时间和宝宝反应。
- **辅食过敏排除闭环**：
  - **喂养记录**：一餐可记录多个食材，餐次与食材在同一事务内一次生效。
  - **餐后反应登记**：须在餐后 72 小时观察窗口内登记；有反应时**只排除该餐中此前未完成安全验证的食材，已确认安全的食材不受影响**；无异常则把食材确认为安全。
  - **状态与原因**：每个食材在每个宝宝档案下有「未验证 / 已确认安全 / 已排除」三种状态，并展示由哪条反应牵连或验证（含餐次时间、症状）。
  - **食谱屏蔽**：按月龄推荐时自动屏蔽含已排除食材的食谱，并列出被屏蔽食谱与命中食材。
  - **撤销误报**：撤销某条反应后，只恢复**不再被其他有效反应牵连**的食材；状态与原因由当前全部有效反应重新推导。
  - **事务与并发**：喂养记录、反应和排除状态一次生效，任一步失败全部回滚；同一餐重复或并发提交只生成一条有效反应（数据库唯一约束 + 行级锁），刷新后结果一致。
- 维护成长里程碑时间线，预留照片上传扩展。
- 统计月度喂养频次、辅食多样性和生长趋势。

## 本地开发方式

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

后端默认连接 MySQL 8.0；本地没有 MySQL 时，可用内置 H2（MySQL 兼容模式）启动，
首次启动自动建表并写入示例食谱，不影响 Docker/MySQL 部署：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.useTestClasspath=true \
  -Dspring-boot.run.arguments=--spring.profiles.active=h2
```

## 过敏闭环 API 一览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/feedings` | 记录一餐（`babyId`、`mealType`、`eatenAt`、`ingredients[]`） |
| GET | `/api/feedings?babyId=` | 喂养记录列表（含食材当前状态与有效反应） |
| POST | `/api/feedings/{feedingId}/reactions` | 72 小时内登记反应（`POSITIVE`/`NEGATIVE`），同餐重复/并发幂等 |
| POST | `/api/feedings/{feedingId}/reactions/{reactionId}/revoke` | 撤销误报，仅恢复不再被牵连的食材 |
| GET | `/api/reactions?babyId=&includeRevoked=` | 反应历史（可含已撤销审计记录） |
| GET | `/api/ingredients/status?babyId=` | 食材状态与原因 |
| GET | `/api/foods/recommend-safe?babyId=&monthAge=` | 按月龄推荐并屏蔽已排除食材 |

## 访问地址

- 前端：http://localhost:18405
- 后端健康检查：http://localhost:19405/health

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vant、Vite、ECharts |
| 后端 | Spring Boot、Java 17、MyBatis-Plus、JWT、SLF4J、Logback、Bean Validation |
| 数据库 | MySQL 8.0（本地开发可用 H2 MySQL 兼容模式） |
| 部署 | Docker Compose、Nginx |

## 项目目录结构

```text
.
├── backend
│   └── src
│       ├── main/java/com/babytracker
│       │   ├── constants/      # 餐次/反应/食材状态枚举与错误码
│       │   ├── controller/     # 喂养、反应、食材状态、食谱推荐等控制器
│       │   ├── entity/         # feeding/meal_reaction/reaction_ingredient/ingredient_status 等
│       │   ├── dto/ vo/        # 请求与响应模型
│       │   ├── exception/      # 统一异常与全局处理
│       │   ├── mapper/         # MyBatis Mapper（含行锁与 upsert）
│       │   ├── service/        # 登记幂等、72h 窗口、撤销重算、推荐屏蔽
│       │   └── utils/          # 食材名称规范化等工具
│       ├── main/resources
│       └── test/               # 过敏闭环集成测试（含并发幂等、回滚）
├── database
│   └── init.sql
├── frontend
│   └── src
│       ├── api/  types/  store/
│       ├── components/         # 喂养卡片、反应登记/撤销、喂养表单
│       └── pages/              # 喂养记录、反应历史、食材状态、食谱推荐、宝宝档案
└── docker-compose.yml
```

## 环境变量说明

| 变量 | 说明 |
| --- | --- |
| COMPOSE_PROJECT_NAME | Compose 项目名，默认 babytracker |
| SPRING_DATASOURCE_URL | Spring Boot MySQL 地址 |
| SPRING_DATASOURCE_USERNAME | 数据库用户名 |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 |
| JWT_SECRET | JWT 签名密钥 |

## Docker 部署说明

- 前端端口：`18405:80`
- 后端端口：`19405:8080`
- MySQL 数据使用命名卷 `babytracker-db-data`。
- Nginx 将 `/api` 代理到 `backend:8080`。

## License

MIT
