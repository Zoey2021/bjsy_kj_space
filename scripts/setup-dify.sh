#!/bin/bash
# 在本机 Docker 部署 Dify（与学习空间端口错开）
# 用法：bash scripts/setup-dify.sh
set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIFY_DIR="$ROOT/deploy/dify"
DIFY_PORT="${DIFY_PORT:-8090}"
ZIP_URL="https://github.com/langgenius/dify/archive/refs/heads/main.zip"

echo "==> 学习空间项目: $ROOT"
echo "==> Dify HTTP 端口: $DIFY_PORT"

if ! docker info >/dev/null 2>&1; then
  echo "错误：请先启动 Docker Desktop。"
  exit 1
fi

mkdir -p "$ROOT/deploy"

install_dify_source() {
  if [ -f "$DIFY_DIR/docker/docker-compose.yaml" ] || [ -f "$DIFY_DIR/docker/docker-compose.yml" ]; then
    echo "==> 已存在 Dify 源码目录，跳过下载"
    return 0
  fi

  rm -rf "$DIFY_DIR"
  mkdir -p "$DIFY_DIR"

  echo "==> 下载 Dify 源码 ZIP（约 50MB，比 git 更稳定）..."
  TMP_ZIP="$(mktemp -t dify).zip"
  if curl -fsSL --retry 3 --retry-delay 5 -o "$TMP_ZIP" "$ZIP_URL"; then
    unzip -q "$TMP_ZIP" -d "$ROOT/deploy"
    mv "$ROOT/deploy/dify-main" "$DIFY_DIR"
    rm -f "$TMP_ZIP"
    echo "==> ZIP 解压成功"
    return 0
  fi

  echo "==> ZIP 失败，尝试 git 浅克隆（最多等待 3 分钟）..."
  if timeout 180 git clone --depth 1 https://github.com/langgenius/dify.git "$DIFY_DIR" 2>/dev/null; then
    return 0
  fi

  echo ""
  echo "自动下载失败。请手动任选一种方式："
  echo "  1) 浏览器打开 $ZIP_URL 下载后解压，文件夹改名为 deploy/dify"
  echo "  2) 终端: git clone --depth 1 https://github.com/langgenius/dify.git deploy/dify"
  echo "  完成后再运行: bash scripts/setup-dify.sh"
  exit 1
}

install_dify_source

COMPOSE_DIR="$DIFY_DIR/docker"
if [ ! -d "$COMPOSE_DIR" ]; then
  echo "错误：未找到 $COMPOSE_DIR"
  exit 1
fi

cd "$COMPOSE_DIR"

if [ ! -f .env ]; then
  cp .env.example .env
fi

patch_env() {
  local key="$1" val="$2"
  if grep -q "^${key}=" .env 2>/dev/null; then
    if [[ "$(uname)" == Darwin ]]; then
      sed -i '' "s|^${key}=.*|${key}=${val}|" .env
    else
      sed -i "s|^${key}=.*|${key}=${val}|" .env
    fi
  else
    echo "${key}=${val}" >> .env
  fi
}

patch_env EXPOSE_NGINX_PORT "$DIFY_PORT"
patch_env EXPOSE_NGINX_SSL_PORT "8443"
patch_env DIFY_BIND_ADDRESS "0.0.0.0"
patch_env CONSOLE_WEB_URL "http://localhost:${DIFY_PORT}"
patch_env CONSOLE_API_URL "http://localhost:${DIFY_PORT}"
patch_env SERVICE_API_URL "http://localhost:${DIFY_PORT}"
patch_env APP_WEB_URL "http://localhost:${DIFY_PORT}"
patch_env APP_API_URL "http://localhost:${DIFY_PORT}"
patch_env FILES_URL "http://localhost:${DIFY_PORT}"
# 国内网络：插件安装模型供应商时需从 PyPI 拉依赖，使用镜像并延长超时
patch_env PIP_MIRROR_URL "https://pypi.tuna.tsinghua.edu.cn/simple"
patch_env PLUGIN_PYTHON_ENV_INIT_TIMEOUT "600"

echo "==> 启动 Dify 容器（首次拉镜像可能 10～20 分钟）..."
docker compose up -d

echo ""
echo "=========================================="
echo "  Dify 控制台: http://localhost:${DIFY_PORT}/install"
echo "  首次请注册管理员，再在「知识库」上传素材"
echo "=========================================="
