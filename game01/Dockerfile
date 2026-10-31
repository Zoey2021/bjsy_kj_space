FROM python:3.10-slim-bookworm

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -i https://pypi.tuna.tsinghua.edu.cn/simple -r requirements.txt

COPY app.py models.py gunicorn_config.py private_key.pem students_export.csv ./
COPY templates ./templates
COPY data ./data

ENV GAME_PREFIX=/pbl-app
ENV PORT=3000
EXPOSE 3000

CMD ["gunicorn", "-c", "gunicorn_config.py", "app:app"]
