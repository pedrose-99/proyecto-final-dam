FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Instalar dependencias de sistema para Chromium (Debian Trixie compatible)
RUN apt-get update && apt-get install -y --no-install-recommends \
    fonts-unifont \
    fonts-liberation \
    libnss3 \
    libnspr4 \
    libatk1.0-0t64 \
    libatk-bridge2.0-0t64 \
    libcups2t64 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2t64 \
    libatspi2.0-0t64 \
    libwayland-client0 \
    && rm -rf /var/lib/apt/lists/*

# Instalar solo el binario de Chromium (deps ya instaladas arriba)
RUN playwright install chromium

COPY app/ ./app/

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
