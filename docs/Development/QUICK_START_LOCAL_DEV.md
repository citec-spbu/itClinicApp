# Быстрый старт локальной разработки

## Настройка Docker

Для работы с локальным бэкендом необходимо настроить Docker Compose для прослушивания на всех сетевых интерфейсах.

### Изменение docker-compose.yaml

Отредактируйте `Registry/docker-compose.yaml`:

```yaml
services:
  server:
    ports:
      - "0.0.0.0:8000:8000"  # Слушать на всех интерфейсах
      
  auth:
    ports:
      - "0.0.0.0:8001:8001"  # Слушать на всех интерфейсах
```

### Перезапуск Docker

```bash
cd Registry
docker-compose down
docker-compose up -d
```

## Настройка бэкенда

### Переменные окружения

Убедитесь, что в `Registry/.env` настроены:

```bash
# Meilisearch
MEILI_MASTER_KEY=your_secure_master_key_at_least_16_chars
MEILISEARCH_API_KEY=your_secure_master_key_at_least_16_chars
MEILISEARCH_HOST=http://meilisearch:7700

# Database
DB_NAME=registry
DB_USER=postgres
DB_PASSWORD=your_db_password

# Redis
REDIS_PASSWORD=your_redis_password

# Auth
TOKEN_SECRET=your_token_secret
COOKIE_SECRET=your_cookie_secret
```

Также добавьте эти переменные в:
- `Registry/server/.env`
- `Registry/strapi/.env`

### Проверка сервисов

```bash
# Проверить статус контейнеров
docker-compose ps

# Проверить Meilisearch
curl http://localhost:7700/health

# Проверить доступность API
curl http://localhost:8000/project/findmany
```

## Запуск приложения

IP адрес определяется автоматически при каждом запуске приложения.

### Android эмулятор

- Автоматически определяется как эмулятор
- Использует `http://10.0.2.2:8000`

### Android реальное устройство

- Автоматически определяется IP адрес компьютера
- Использует динамически определенный IP из активного сетевого интерфейса

### iOS симулятор

- Использует `http://localhost:8000`

### iOS реальное устройство

- Использует IP адрес из `LocalDevConfig.LOCAL_MACHINE_IP`

## Проверка подключения

### В приложении

Откройте `NetworkDebugScreen` для просмотра:
- Автоматически определенного IP
- Всех сетевых интерфейсов
- Настроек подключения

### В логах

После запуска приложения в логах выводится:

```
=== API Configuration ===
Use Local API: true
Is Emulator: true/false
Local Host: 10.0.2.2 или IP адрес
Base URL: http://...
=======================
```

### Через терминал

```bash
# Проверить доступность API
curl http://localhost:8000/project/findmany

# Для реального устройства (замените IP)
curl http://YOUR_IP:8000/project/findmany
```

## Решение проблем

### Connection refused или timeout

1. Проверьте, что Docker контейнеры запущены:
   ```bash
   docker-compose ps
   ```

2. Проверьте, что порты слушаются на всех интерфейсах:
   ```bash
   netstat -an | grep 8000
   ```

3. Убедитесь, что устройство и компьютер в одной сети

4. Проверьте firewall (macOS):
   ```bash
   # Временно отключить для теста
   sudo pfctl -d
   ```

### Эмулятор не подключается

1. Убедитесь, что используется правильный адрес: `10.0.2.2` для Android эмулятора
2. Проверьте `network_security_config.xml` - должен разрешать HTTP трафик к `10.0.2.2`

### Реальное устройство не подключается

1. Проверьте IP адрес компьютера:
   ```bash
   ifconfig | grep "inet " | grep -v 127.0.0.1
   ```

2. Убедитесь, что устройство в той же Wi-Fi сети

3. Проверьте, что Docker слушает на `0.0.0.0`, а не только на `127.0.0.1`

## Переключение на продакшн API

В `ApiConfig.kt`:

```kotlin
private const val USE_LOCAL_API = false  // Использовать https://citec.spb.ru/api
```
