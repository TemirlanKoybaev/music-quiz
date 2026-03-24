# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Сборка
mvn clean package

# Запуск
mvn spring-boot:run

# Тесты
mvn test

# Один тест-класс
mvn test -Dtest=MusicQuizApplicationTests
```

## Architecture

Spring Boot 3 / Java 17 / Maven. H2 (file-based) через JPA.

**Packages** (`src/main/java/com/musicquiz/`):

| Пакет | Назначение |
|---|---|
| `model/` | JPA-сущности: `Song`, `QuizSession`, `QuizQuestion` |
| `repository/` | Spring Data JPA репозитории |
| `service/` | Бизнес-логика: `SongService` (загрузка файлов), `QuizService` (логика викторины) |
| `controller/` | REST-контроллеры + `GlobalExceptionHandler` |
| `dto/` | Request/Response объекты (не сущности JPA) |

**Поток данных викторины:**
1. `POST /api/songs/upload` — загрузить аудиофайлы (нужно минимум `app.quiz.options-per-question` песен)
2. `POST /api/quiz/start` — создать `QuizSession` с `QuizQuestion`-ами (каждый вопрос содержит 4 варианта)
3. `GET /api/quiz/{id}/question/{n}` → `songId` → `GET /api/songs/{songId}/audio` — получить вопрос и стримить аудио
4. `POST /api/quiz/{id}/answer/{n}` — проверка ответа, правильный ответ возвращается в ответе
5. `POST /api/quiz/{id}/finish` — завершить сессию
6. `GET /api/quiz/history` — история всех сессий

**Хранение файлов:** аудиофайлы сохраняются на диск (`./uploads/audio/`), в БД хранится только путь.

## Key config (`application.properties`)

- `app.audio.upload-dir` — директория для аудиофайлов
- `app.quiz.default-questions` — кол-во вопросов по умолчанию
- `app.quiz.options-per-question` — кол-во вариантов ответа (4)
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/musicquiz`)
