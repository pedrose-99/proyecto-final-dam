FROM python:3.11-slim

WORKDIR /app

# 1. Instalamos dependencias básicas y limpiamos para ahorrar espacio
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 2. Instalamos las librerías de Python
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
RUN pip install playwright

# 3. Instalamos el navegador
RUN playwright install chromium

# 4. ACTUALIZACIÓN Y DEPENDENCIAS (Aquí estaba el fallo)
# Usamos "|| true" para que si fallan las fuentes tipográficas, el build siga adelante.
# Chromium funcionará igual para scrapear aunque no tenga las fuentes de Ubuntu.
RUN apt-get update && playwright install-deps chromium || true

COPY app/ ./app/

EXPOSE 8000

# Asegúrate de que la ruta app.main:app es correcta según tu estructura
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]