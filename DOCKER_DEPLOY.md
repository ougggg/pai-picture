# Docker 本地部署指南

## 前置要求

1. **安装 Docker Desktop**
   - Windows: 下载并安装 [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
   - 确保 Docker 服务已启动

2. **本地数据库准备**
   - MySQL 运行在 `localhost:3306`
   - Redis 运行在 `localhost:6379`
   - 确保已创建数据库 `demo_picture`
   - 确保 MySQL root 密码为 `123456`（或修改 `docker-compose.yml` 中的配置）

## 快速开始

### 1. 初始化数据库（首次部署）

如果数据库未创建，执行以下 SQL：

```sql
create database if not exists `demo_picture` default character set utf8mb4 collate utf8mb4_unicode_ci;
```

然后执行 `picture-backend/sql/create_table.sql` 初始化表结构。

### 2. 配置生产环境文件

参考 [配置说明 - 配置生产环境文件](#配置生产环境文件首次部署必做)

### 3. 启动服务

在项目根目录执行：

```bash
docker-compose up -d --build
```

首次构建会下载依赖和镜像，需要几分钟时间。

### 4. 访问应用

- **前端地址**: http://localhost
- **后端 API**: http://localhost:8123/api
- **接口文档**: http://localhost:8123/api/doc.html

## 常用命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看后端日志
docker-compose logs -f backend

# 查看前端日志
docker-compose logs -f frontend
```

### 停止服务
```bash
docker-compose down
```

### 重启服务
```bash
docker-compose restart
```

### 重新构建并启动
```bash
docker-compose up -d --build
```

### 查看容器资源使用
```bash
docker stats
```

## 配置说明

### 配置生产环境文件（首次部署必做）

由于 `application-prod.yml` 包含敏感信息，未提交到代码库。首次部署需要：

1. **复制模板文件**：
   ```bash
   cp picture-backend/src/main/resources/application-prod.yml.example picture-backend/src/main/resources/application-prod.yml
   ```

2. **配置敏感信息**（两种方式任选其一）：

   **方式一：直接编辑文件**
   
   编辑 `picture-backend/src/main/resources/application-prod.yml`，填写：
   - COS 配置（腾讯云对象存储）
   - 阿里云 AI API Key
   
   **方式二：通过环境变量配置（推荐）**
   
   在 `docker-compose.yml` 的 `backend` 服务中添加环境变量：
   ```yaml
   environment:
     # ... 其他环境变量
     COS_HOST: your-cos-host
     COS_SECRET_ID: your-secret-id
     COS_SECRET_KEY: your-secret-key
     COS_REGION: your-region
     COS_BUCKET: your-bucket
     ALIYUN_AI_API_KEY: your-api-key
   ```

   > **注意**：如果不需要使用 COS 或阿里云 AI 功能，可以留空，但相关功能将无法使用。

### 修改数据库密码

如果本地 MySQL 密码不是 `123456`，编辑 `docker-compose.yml`：

```yaml
environment:
  SPRING_DATASOURCE_PASSWORD: 你的密码
```

### 修改端口

如需修改端口，编辑 `docker-compose.yml` 中的 `ports` 配置：

```yaml
backend:
  ports:
    - "8123:8123"  # 格式: "宿主机端口:容器端口"

frontend:
  ports:
    - "80:80"
```

## 故障排查

### 1. 端口被占用

如果 80 或 8123 端口被占用：

- 修改 `docker-compose.yml` 中的端口映射
- 或停止占用端口的服务

### 2. 数据库连接失败

检查：
- MySQL 是否正在运行
- 数据库 `demo_picture` 是否已创建
- 密码是否正确
- MySQL 是否允许外部连接（Docker 通过 `host.docker.internal` 访问）

### 3. 容器启动失败

查看详细日志：
```bash
docker-compose logs backend
docker-compose logs frontend
```

### 4. 前端无法访问后端

检查：
- 后端容器是否正常运行：`docker-compose ps`
- 后端日志是否有错误：`docker-compose logs backend`
- 浏览器控制台是否有错误信息

### 5. 重新构建镜像

如果代码有更新，需要重新构建：

```bash
docker-compose down
docker-compose up -d --build
```

## 项目结构

```
pai-picture/
├── docker-compose.yml          # Docker 编排配置
├── picture-backend/
│   ├── Dockerfile              # 后端镜像构建文件
│   └── sql/                    # 数据库初始化脚本
└── picture-frontend/
    ├── Dockerfile              # 前端镜像构建文件
    └── nginx.conf              # Nginx 配置
```

## 注意事项

1. **数据持久化**: 使用本地 MySQL 和 Redis，数据保存在本地数据库中
2. **首次启动**: 首次构建需要下载镜像和依赖，耗时较长
3. **网络访问**: 容器通过 `host.docker.internal` 访问宿主机服务
4. **生产环境**: 如需公网访问，请配置内网穿透或部署到云服务器

