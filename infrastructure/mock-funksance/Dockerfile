FROM python:3.12-alpine
WORKDIR /app
COPY server.py .
EXPOSE 8080
USER nobody
ENV MOCK_PORT=8080
CMD ["python", "server.py"]