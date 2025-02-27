# Интеграционные тесты на Scala

Пример проекта с интеграционными тестами с использованием testcontainers на Scala.

Локальная сборка:

```
sbt clean compile cards-app / test docker:publishLocal
```

Локальный запуск после сборки:

```
docker compose up
```

Запуск API тестов после сборки

```
sbt cards-app-tests/test
```