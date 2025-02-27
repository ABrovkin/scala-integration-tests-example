# Интеграционные тесты на Scala

Пример проекта с интеграционными тестами с использованием testcontainers на Scala.

Локальная сборка:

```
sbt clean compile test docker:publishLocal
```

Локальный запуск после сборки:

```
docker compose up
```