# 宝宝成长记录与辅食管理平台

帮助新手父母管理宝宝档案、生长曲线、疫苗计划、辅食食谱和喂养记录。

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
- **辅食过敏排除闭环**：每餐记录多个食材；餐后 72 小时内登记反应，只排除该餐中此前未安全验证的食材，已确认安全的食材不受影响；食谱推荐自动屏蔽已排除食材；支持撤销误报，仅恢复不再被其他有效反应牵连的食材，并展示每种食材的状态与原因。
- 喂养记录、反应登记与排除状态在同一事务内一次生效，任一步失败全部回滚；同一餐重复或并发提交只生成一条有效反应。
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

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vant、Vite、ECharts |
| 后端 | Spring Boot、Java 17、MyBatis-Plus、JWT、SLF4J、Logback |
| 数据库 | MySQL 8.0 |
| 部署 | Docker Compose、Nginx |

## 项目目录结构

```text
.
├── backend
│   └── src/main
│       ├── java/com/babytracker
│       │   ├── constants
│       │   ├── controller
│       │   ├── entity
│       │   ├── exception
│       │   ├── mapper
│       │   ├── service
│       │   └── utils
│       └── resources
├── database
├── frontend
│   └── src
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
