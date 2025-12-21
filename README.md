# 云图库系统 (Pai Picture)

一个基于前后端分离架构的现代化智能图片管理平台，集成了 AI 智能识别、多模态搜索、三级缓存架构等核心功能。

## ✨ 核心特性

- 🤖 **AI 智能识别**：集成阿里云通义千问，自动标注标签与分类
- ⚡ **极致性能**：Caffeine + Redis + MySQL 三级缓存架构，应对高并发查询
- 🔍 **多模态搜索**：支持关键词、分类、以图搜图及独创的以色搜图算法
- 📦 **多空间管理**：支持普通版、专业版、旗舰版多级空间配额管理
- 🎨 **智能加工**：AI 扩图、风格重绘、图片裁剪等丰富功能
- 📊 **数据可视化**：空间使用率、分类占比、标签词云等分析看板

## 🛠️ 技术栈

### 前端
- **框架**: Vue 3 + TypeScript + Vite
- **UI 组件库**: Ant Design Vue
- **状态管理**: Pinia
- **图表库**: ECharts
- **路由**: Vue Router 4

### 后端
- **框架**: Spring Boot 2.7.6
- **数据库**: MySQL 8.0
- **ORM**: MyBatis Plus 3.5.9
- **缓存**: Redis + Caffeine（本地缓存）
- **对象存储**: 腾讯云 COS
- **AI 服务**: 阿里云通义千问（图片识别、扩图、生图）
- **接口文档**: Knife4j 4.4.0

## 📁 项目结构

```
pai-picture/
├── picture-backend/          # 后端服务
│   ├── src/main/java/
│   │   └── com/ouguofeng/
│   │       ├── annotation/   # 自定义注解
│   │       ├── aop/          # 切面编程
│   │       ├── api/          # 第三方 API 封装
│   │       ├── config/       # 配置类
│   │       ├── controller/   # 控制器层
│   │       ├── manager/      # 第三方 SDK 封装
│   │       ├── mapper/       # 数据访问层
│   │       ├── model/        # 数据模型
│   │       ├── service/      # 业务逻辑层
│   │       └── utils/        # 工具类
│   └── sql/                  # 数据库脚本
├── picture-frontend/         # 前端应用
│   ├── src/
│   │   ├── api/              # API 接口（自动生成）
│   │   ├── components/       # 组件
│   │   ├── pages/            # 页面
│   │   ├── stores/           # 状态管理
│   │   └── utils/            # 工具函数
│   ├── Dockerfile            # 前端 Docker 镜像构建文件
│   ├── nginx.conf            # Nginx 配置
│   └── openapi.config.js     # OpenAPI 配置
├── docker-compose.yml        # Docker Compose 编排文件
├── DOCKER_DEPLOY.md          # Docker 部署文档
└── README.md
```

## 🚀 快速开始

### 环境要求

**方式一：Docker 部署（推荐）**
- Docker Desktop
- MySQL 8.0+（本地运行）
- Redis 6.0+（本地运行）

**方式二：本地开发**
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+

---

## 🐳 Docker 一键部署

### 前置准备

1. **安装 Docker Desktop**
   - Windows: [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
   - 确保 Docker 服务已启动

2. **本地数据库准备**
   - MySQL 运行在 `localhost:3306`
   - Redis 运行在 `localhost:6379`
   - 创建数据库：`create database if not exists demo_picture default character set utf8mb4 collate utf8mb4_unicode_ci;`
   - 执行 `picture-backend/sql/create_table.sql` 初始化表结构

### 部署步骤

1. **配置生产环境文件**
   ```bash
   # 复制模板文件
   cp picture-backend/src/main/resources/application-prod.yml.example \
      picture-backend/src/main/resources/application-prod.yml
   
   # 编辑配置文件，填写 COS 和阿里云 AI 配置
   # 或通过 docker-compose.yml 中的环境变量配置
   ```

2. **一键启动**
   ```bash
   docker-compose up -d --build
   ```

3. **访问应用**
   - 前端：http://localhost
   - 后端 API：http://localhost:8123/api
   - 接口文档：http://localhost:8123/api/doc.html

> 📖 详细部署说明请参考 [DOCKER_DEPLOY.md](./DOCKER_DEPLOY.md)

---

## 💻 本地开发部署

### 后端启动

1. 导入数据库脚本
```bash
cd picture-backend/sql
# 执行 create_table.sql
```

2. 配置数据库和 Redis
```yaml
# picture-backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/demo_picture
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

3. 配置腾讯云 COS 和阿里云 AI

   创建 `application-local.yml` 文件：
   ```yaml
   cos:
     client:
       host: your_cos_host
       secretId: your_secret_id
       secretKey: your_secret_key
       region: your_region
       bucket: your_bucket
   
   aliYunAi:
     apiKey: your_api_key
   ```
   
   > 💡 提示：可以参考 `application.yml` 中的配置结构，或参考 `application-prod.yml.example` 模板文件

4. 启动后端服务
```bash
cd picture-backend
mvn spring-boot:run
```

### 前端启动

1. 安装依赖
```bash
cd picture-frontend
npm install
```

2. 生成 API 文件（首次启动）
```bash
npm run openapi
```

3. 启动开发服务器
```bash
npm run dev
```

## 📋 功能模块

### 用户模块
- 用户注册/登录
- 权限管理（普通用户/管理员）
- 用户关注/粉丝系统

### 图片模块
- 多方式上传（本地文件、URL 抓取）
- 批量上传与处理
- 图片管理（编辑、删除、收藏、点赞）
- AI 智能识别与标注
- AI 扩图与风格重绘
- 图片裁剪

### 空间模块
- 多级空间管理（普通版/专业版/旗舰版）
- 空间配额控制
- 空间数据分析
- 空间排行

### 搜索模块
- 关键词搜索
- 分类筛选
- 以色搜图（基于 RGB 空间欧氏距离）
- 以图搜图

### 分析模块
- 空间使用率统计
- 图片分类占比
- 标签词云
- 用户行为分析

## 🏗️ 架构设计

### 三级缓存架构

```
查询流程：
1. Caffeine 本地缓存（L1）- 零延迟访问
2. Redis 分布式缓存（L2）- 跨实例共享
3. MySQL 数据库（L3）- 持久化存储
```

### 核心优化策略

- **缓存雪崩防护**：随机 TTL 避免集中失效
- **缓存击穿防护**：分布式锁 + 双重检查锁（DCL）
- **高并发上传**：线程池 + CompletableFuture 异步处理
- **AI 异步处理**：快速入库 + 后台异步识别

## 📝 配置说明

### 配置文件说明

项目使用 Spring Boot 多环境配置：

- `application.yml` - 基础配置（已提交）
- `application-local.yml` - 本地开发配置（需自行创建，已加入 .gitignore）
- `application-prod.yml` - 生产环境配置（需自行创建，已加入 .gitignore）
- `application-prod.yml.example` - 生产环境配置模板（已提交，可参考）

### 必需配置项

1. **数据库配置**（`application.yml` 或 `application-local.yml`）
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/demo_picture
       username: your_username
       password: your_password
     redis:
       host: localhost
       port: 6379
   ```

2. **腾讯云 COS 配置**（必需，用于图片存储）
   ```yaml
   cos:
     client:
       host: your_cos_host
       secretId: your_secret_id
       secretKey: your_secret_key
       region: your_region
       bucket: your_bucket
   ```

3. **阿里云 AI 配置**（必需，用于 AI 功能）
   ```yaml
   aliYunAi:
     apiKey: your_api_key
   ```

### Docker 部署配置

Docker 部署时，敏感信息可通过环境变量注入，详见 `docker-compose.yml` 和 `DOCKER_DEPLOY.md`。

## 📚 相关文档

- [Docker 部署指南](./DOCKER_DEPLOY.md) - 详细的 Docker 部署说明

## 📄 许可证

本项目采用开源许可证，详见 LICENSE 文件。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**注意**：首次部署时，请确保已创建数据库并执行初始化脚本，同时配置好 COS 和阿里云 AI 相关服务。