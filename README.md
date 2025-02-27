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

Интеграционные тесты с мокированным окружением контроллера находятся [тут](cards-app/src/test/scala/com/abrovkin/http/CardControllerSpec.scala)

API-тесты, запускаемые против приложения в мокированном окружении, находятся [тут](cards-app-tests/src/test/scala/com/abrovkin/CardsServiceSpec.scala)