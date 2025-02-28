# Интеграционные тесты на Scala

Пример проекта с интеграционными тестами с использованием testcontainers на Scala.

Локальная сборка и прогон тестов:

```
sbt clean compile cards-app/test cards-app/docker:publishLocal cards-app-tests/test
```

Локальный запуск после сборки:

```
docker compose up
```

Интеграционные тесты с мокированным окружением контроллера находятся [тут](cards-app/src/test/scala/com/abrovkin/http/CardControllerSpec.scala)

API-тесты, запускаемые против приложения в мокированном окружении, находятся [тут](cards-app-tests/src/test/scala/com/abrovkin/CardsServiceSpec.scala)