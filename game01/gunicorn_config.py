# Gunicorn 配置文件
# 用于生产环境运行 Flask 应用

import multiprocessing
import os

# 服务器配置
bind = "0.0.0.0:3000"  # 监听所有IP的3000端口
# SQLite 不支持真正的并发写入，但读取可以并发
# CPU使用率已经达到100%，需要大幅减少worker数量
# 优化：使用2个worker，优先保证CPU不超载
cpu_count = multiprocessing.cpu_count()
workers = 2  # 固定使用2个worker，大幅降低CPU使用率
worker_class = "sync"  # 工作模式
worker_connections = 1000
timeout = 30  # 超时时间（秒）- 减少到30秒，快速响应，避免请求堆积
keepalive = 5

# 日志配置
logdir = os.path.join(os.path.dirname(__file__), "logs")
os.makedirs(logdir, exist_ok=True)
accesslog = os.path.join(logdir, "gunicorn_access.log")  # 访问日志
errorlog = os.path.join(logdir, "gunicorn_error.log")  # 错误日志
loglevel = "info"  # 日志级别: debug, info, warning, error, critical
access_log_format = '%(h)s %(l)s %(u)s %(t)s "%(r)s" %(s)s %(b)s "%(f)s" "%(a)s"'

# 进程配置
daemon = False  # 不以守护进程运行（由systemd管理）
pidfile = "gunicorn.pid"  # PID文件路径
umask = 0o007

# 性能优化
preload_app = True  # 预加载应用
max_requests = 1000  # 每个worker处理的最大请求数
max_requests_jitter = 50  # 随机抖动，避免所有worker同时重启

# 应用配置
wsgi_app = "app:app"  # Flask应用入口

