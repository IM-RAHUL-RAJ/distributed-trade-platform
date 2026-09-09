FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN useradd --system --uid 10001 appuser && mkdir -p output && chown -R appuser output
USER appuser
ENTRYPOINT ["python", "main.py"]