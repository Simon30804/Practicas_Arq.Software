FROM python:3.9-slim

WORKDIR /app

COPY broker_server.py .
COPY cliente.py .

EXPOSE 5555

CMD ["python", "-u", "broker_server.py"]