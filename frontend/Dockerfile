# 第一阶段：构建前端
FROM node:18-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install --registry=https://registry.npmmirror.com
COPY . .
RUN npm run build

# 第二阶段：Nginx 托管静态文件
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
# 构建时 PDF 可能为 600，nginx 进程无法读取会 403
RUN find /usr/share/nginx/html/textbooks -name '*.pdf' -exec chmod a+r {} \; 2>/dev/null || true
EXPOSE 80
