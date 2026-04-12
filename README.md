# itClinicApp

Kotlin Multiplatform (Android + iOS) приложение для IT‑клиники СПбГУ.

> Подробная документация по экранам и сценариям: [docs/Screens/](docs/Screens/).

---

## Содержание

- [О проекте](#о-проекте)
- [Планы развития](#планы-развития)
- [Роли пользователей](#роли-пользователей)
- [Авторизация и доступ](#авторизация-и-доступ)
- [Навигация по приложению](#навигация-по-приложению)
- [Пользовательские сценарии](#пользовательские-сценарии)
- [Документация экранов](#документация-экранов)
- [Структура репозитория](#структура-репозитория)
- [Дизайн](#дизайн)
- [Сборка и запуск](#сборка-и-запуск)
- [Troubleshooting](#troubleshooting)

---

## О проекте

**itClinicApp** — приложение, в котором можно:

- просматривать проекты IT‑клиники и детали проектов;
- (для заказчиков) оставлять заявку на новый проект;
- смотреть рейтинг проектов и студентов по метрикам активности репозитория;
- смотреть личную и проектную статистику (commits / issues / pull requests и производные метрики);
- управлять профилем и базовыми настройками приложения;
- отправлять обратную связь.

---

## Планы развития

- **Английский язык** — добавление полноценной локализации интерфейса.
- **Тёмная тема** — поддержка светлой/тёмной темы оформления.
- **Новая вкладка сравнения** — возможность сравнивать **проекты** и **пользователей** по различным метрикам (сводные таблицы/рейтинги, фильтры, выбор периодов).

---

## Роли пользователей

В приложении предполагаются разные сценарии для:

- **Студентов** — просмотр проектов, участие в командах, просмотр рейтингов и статистики.
- **Руководителей / наставников** — просмотр проектов, статистики и состава команды.
- **Заказчиков** — возможность предложить проект через форму заявки.

---

## Авторизация и доступ

Приложение поддерживает два режима:

1. **С авторизацией (GitHub)** — доступен полный функционал.
2. **Без авторизации** — доступен ограниченный просмотр.

### Ограничения для неавторизованных пользователей

Для пользователя **без авторизации** действуют ограничения:

1. **Вкладка 2 (Рейтинг/Статистика) полностью недоступна**.
   - нельзя открыть рейтинги проектов/студентов;
   - нельзя открывать экран фильтров метрик;
   - нельзя открывать личную/проектную статистику и детальные экраны метрик;
   - функции экспорта статистики (PDF/Excel) также недоступны.

2. На экранах **«Просмотр проекта»** и **«Мой проект»** доступна информация **только до блока “Требования проекта” (не включительно)**.
   - то есть можно увидеть карточку проекта, статус и **«Описание проекта»**;
   - всё, что начинается с **«Требования проекта»** и ниже (требования, требования для исполнителей, команда и т.д.) — скрыто.

---

## Навигация по приложению

Приложение логически разделено на 3 раздела (вкладки):

1. **Проекты** — список проектов, фильтрация, просмотр проекта, «Мой проект».
2. **Рейтинг / Статистика** — рейтинги проектов/студентов, фильтры, личная и проектная статистика, детальные экраны метрик.
3. **Информация** — профиль, настройки, политика конфиденциальности, обратная связь.

> Примечание: для пользователя **без авторизации** вкладка **2** скрыта/заблокирована (см. [ограничения](#ограничения-для-неавторизованных-пользователей)).

---

## Пользовательские сценарии

### Базовый сценарий (авторизованный пользователь)

1. Первый запуск → [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) (войти через GitHub или продолжить без авторизации)
2. Повторный запуск → [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md)
3. Выбор проекта → [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md)
4. Переход к статистике:
   - личная: [docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md)
   - проектная: [docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md)
5. Детализация метрики → соответствующий экран из [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/)
6. Информация → [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) → профиль/настройки/политика/обратная связь

### Базовый сценарий (неавторизованный пользователь)

1. Первый запуск → [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) → **«Продолжить без авторизации»**
2. Повторный запуск → [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md)
3. Выбор проекта → [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md)
4. Просмотр доступной части проекта → **до блока “Требования проекта” (не включительно)**
5. Информация → [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) → политика/обратная связь

---

## Документация экранов

> Все пути указаны с префиксом `docs/Screens/`.

### Первый запуск

- [docs/Screens/initialScreen.md](docs/Screens/initialScreen.md) — загрузочный экран.
- [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) — экран авторизации (GitHub / без авторизации).

### Вкладка 1 — Проекты

- [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md) — список проектов.
- [docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsFiltersAlert.md](docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsFiltersAlert.md) — модальное окно фильтров списка проектов.
- [docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsCustomersAlert.md](docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsCustomersAlert.md) — модальное окно заявки для заказчиков.
- [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md) — просмотр выбранного проекта.
- [docs/Screens/Tabs/1stTab/MyProjectScreen/MyProjectScreen.md](docs/Screens/Tabs/1stTab/MyProjectScreen/MyProjectScreen.md) — «Мой проект».

### Вкладка 2 — Рейтинг

- [docs/Screens/Tabs/2ndTab/RatingScreens/RatingProjectsScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/RatingProjectsScreen.md) — рейтинг проектов.
- [docs/Screens/Tabs/2ndTab/RatingScreens/RatingStudentsScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/RatingStudentsScreen.md) — рейтинг студентов.

#### Фильтры рейтинга

- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersScreen.md) — экран фильтров/метрик.
- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersSaveTemplateAlert.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersSaveTemplateAlert.md) — модальное окно «Сохранить шаблон».
- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersMetricInfoAlert.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersMetricInfoAlert.md) — модальное окно пояснения к метрике.

### Вкладка 2 — Статистика

- [docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md) — личная статистика.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md) — статистика проекта.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/StatistiscScreenSettings.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/StatistiscScreenSettings.md) — настройка отображаемых вкладок статистики.

#### Детальные экраны метрик (кнопка «Подробнее»)

- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CommitsDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CommitsDetailsScreen.md) — коммиты.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/IssueDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/IssueDetailsScreen.md) — issues.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/PullRequestDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/PullRequestDetailsScreen.md) — pull requests.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/FastPRDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/FastPRDetailsScreen.md) — быстрые PR.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/RefactoringDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/RefactoringDetailsScreen.md) — изменчивость кода (рефакторинг).
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CodeOwnershipDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CodeOwnershipDetailsScreen.md) — владение кодом.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/DominantDayDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/DominantDayDetailsScreen.md) — доминирующий день недели.

### Вкладка 3 — Информация

- [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) — экран «Информация».
- [docs/Screens/Tabs/3rdTab/SettingsScreen.md](docs/Screens/Tabs/3rdTab/SettingsScreen.md) — настройки приложения.
- [docs/Screens/Tabs/3rdTab/PrivacyPolicy.md](docs/Screens/Tabs/3rdTab/PrivacyPolicy.md) — политика конфиденциальности.

#### Профиль

- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileScreen.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileScreen.md) — профиль.
- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileEditAlert.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileEditAlert.md) — редактирование профиля (модальное окно).
- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileLogoutConfirmAlert.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileLogoutConfirmAlert.md) — подтверждение выхода.

#### Обратная связь

- [docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackScreen.md](docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackScreen.md) — форма обратной связи.
- [docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackThankYouAlert.md](docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackThankYouAlert.md) — благодарность после отправки.

### Общая схема

- [docs/scheme.md](docs/scheme.md) — общая схема/структура (если используется в проекте).

---

## Структура репозитория

Проект — **Kotlin Multiplatform** с целями **Android** и **iOS**.

- `composeApp/` — общий модуль Compose Multiplatform
  - `src/commonMain/` — общий код для всех платформ
  - `src/androidMain/` — Android-специфичный код
  - `src/iosMain/` — iOS-специфичный код
  - `src/jvmMain/` — JVM/desktop-специфичный код (если используется)
- `iosApp/` — iOS приложение/точка входа (Swift/Xcode)

---

## Дизайн

- Figma: https://www.figma.com/design/Pac1BvaweGNNmjDnvW2ZdL/IT-CLINIC?node-id=0-1&p=f&t=Se6QVT4BCQxH8jUh-0

---

## 🔐 Конфигурация для разработчиков

> **Важно**: Перед первым запуском необходимо настроить конфигурацию безопасности!

После клонирования репозитория:

```bash
# 1. Скопируйте шаблон конфигурации
cd composeApp/src/commonMain/kotlin/com/spbu/projecttrack/
cp BuildConfig.example.kt BuildConfig.kt

# 2. Сгенерируйте тестовый токен (для локальной разработки)
cd Registry/
npm install jsonwebtoken
node generate-test-token.js

# 3. Вставьте полученный токен в BuildConfig.kt
```

Файл `BuildConfig.kt` содержит токены и ключи и **не коммитится в Git**.

📖 Подробная инструкция: [composeApp/SECURITY_README.md](composeApp/SECURITY_README.md)

---

## Сборка и запуск

### Требования

- Android Studio (для Android)
- Xcode (для iOS)
- JDK (обычно ставится вместе с Android Studio)

### Android

Собрать debug-версию:

- macOS/Linux:
  ```sh
  ./gradlew :composeApp:assembleDebug
  ```

- Windows:
  ```bat
  .\gradlew.bat :composeApp:assembleDebug
  ```

Запуск удобнее делать через Android Studio (Run конфигурация).

### iOS

- Откройте папку `iosApp/` в Xcode и запустите приложение.

---

## Troubleshooting

- **Gradle не собирается / странные зависимости**: попробуйте `./gradlew clean` и повторную сборку.
- **iOS не запускается**: проверьте, что установлен Xcode и выбрана актуальная версия симулятора.
- **Не видно изменений в UI**: убедитесь, что перезапущена сборка/приложение (иногда помогает invalidate caches в IDE).

## GitHub Actions CI/CD

- Подробная документация вынесена в [docs/Development/GITHUB_CICD.md](docs/Development/GITHUB_CICD.md).
- Там описаны workflow, GHCR, причины `unauthorized`, публикация образа и стратегия отката.

# itClinicApp (EN)

## Table of Contents

- [About](#about)
- [Planned Features](#planned-features)
- [User Roles](#user-roles)
- [Authentication and Access](#authentication-and-access)
- [App Navigation](#app-navigation)
- [User Flows](#user-flows)
- [Screen Documentation](#screen-documentation)
- [Repository Structure](#repository-structure)
- [Design](#design)
- [Build and Run](#build-and-run)
- [Troubleshooting](#troubleshooting)

---

## About

**itClinicApp** is an application where you can:

- browse IT Clinic projects and project details;
- (for customers) submit a new project request;
- view project and student rankings based on repository activity metrics;
- view personal and project statistics (commits / issues / pull requests and derived metrics);
- manage your profile and basic app settings;
- send feedback.

---

## Planned Features

- **English language** — full UI localization.
- **Dark theme** — support for light/dark appearance.
- **New comparison tab** — compare **projects** and **users** across different metrics (summary tables/rankings, filters, and period selection).

---

## User Roles

The app supports different scenarios for:

- **Students** — browsing projects, participating in teams, viewing rankings and statistics.
- **Leaders / Mentors** — viewing projects, statistics, and team composition.
- **Customers** — ability to propose a project via a request form.

---

## Authentication and Access

The app supports two modes:

1. **With authentication (GitHub)** — full functionality available.
2. **Without authentication** — limited viewing access.

### Restrictions for Unauthenticated Users

For users **without authentication**, the following restrictions apply:

1. **Tab 2 (Ranking/Statistics) is completely unavailable**.
   - cannot open project/student rankings;
   - cannot open the metric filters screen;
   - cannot open personal/project statistics and detailed metric screens;
   - export functions for statistics (PDF/Excel) are also unavailable.

2. On the **Project View** and **My Project** screens, information is available **only up to the “Project requirements” block (not included)**.
   - that is, you can see the project card, status, and **“Project description”**;
   - everything starting from **“Project requirements”** and below (requirements, requirements for performers, team, etc.) is hidden.

---

## App Navigation

The app is logically divided into 3 sections (tabs):

1. **Projects** — project list, filtering, project view, “My Project”.
2. **Ranking / Statistics** — project/student rankings, filters, personal and project statistics, detailed metric screens.
3. **Information** — profile, settings, privacy policy, feedback.

> Note: For users **without authentication**, tab **2** is hidden/disabled (see [restrictions](#restrictions-for-unauthenticated-users)).

---

## User Flows

### Basic Flow (Authenticated User)

1. First launch → [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) (sign in via GitHub or continue without authentication)
2. Subsequent launches → [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md)
3. Select a project → [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md)
4. Navigate to statistics:
   - personal: [docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md)
   - project: [docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md)
5. Metric details → corresponding screen from [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/)
6. Information → [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) → profile/settings/privacy/feedback

### Basic Flow (Unauthenticated User)

1. First launch → [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) → **“Continue without authentication”**
2. Subsequent launches → [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md)
3. Select a project → [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md)
4. View available part of the project → **up to the “Project requirements” block (not included)**
5. Information → [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) → privacy/feedback

---

## Screen Documentation

> All paths are prefixed with `docs/Screens/`.

### First Launch

- [docs/Screens/initialScreen.md](docs/Screens/initialScreen.md) — splash screen.
- [docs/Screens/AuthScreen.md](docs/Screens/AuthScreen.md) — authentication screen (GitHub / without authentication).

### Tab 1 — Projects

- [docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md](docs/Screens/Tabs/1stTab/AllProjectScreen/AllProjects.md) — projects list.
- [docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsFiltersAlert.md](docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsFiltersAlert.md) — projects list filter modal.
- [docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsCustomersAlert.md](docs/Screens/Tabs/1stTab/AllProjectScreen/ProjectsCustomersAlert.md) — customer request modal.
- [docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md](docs/Screens/Tabs/1stTab/ProjectViewScreen/ProjectViewScreen.md) — selected project view.
- [docs/Screens/Tabs/1stTab/MyProjectScreen/MyProjectScreen.md](docs/Screens/Tabs/1stTab/MyProjectScreen/MyProjectScreen.md) — “My Project”.

### Tab 2 — Ranking

- [docs/Screens/Tabs/2ndTab/RatingScreens/RatingProjectsScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/RatingProjectsScreen.md) — project ranking.
- [docs/Screens/Tabs/2ndTab/RatingScreens/RatingStudentsScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/RatingStudentsScreen.md) — student ranking.

#### Ranking Filters

- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersScreen.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersScreen.md) — filters/metrics screen.
- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersSaveTemplateAlert.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersSaveTemplateAlert.md) — “Save template” modal.
- [docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersMetricInfoAlert.md](docs/Screens/Tabs/2ndTab/RatingScreens/Filters/FiltersMetricInfoAlert.md) — metric explanation modal.

### Tab 2 — Statistics

- [docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/PersonalStatisticsScreen.md) — personal statistics.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/ProjectStatisticsScreen.md) — project statistics.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/StatistiscScreenSettings.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/StatistiscScreenSettings.md) — statistics tabs settings.

#### Detailed Metric Screens (Details button)

- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CommitsDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CommitsDetailsScreen.md) — commits.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/IssueDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/IssueDetailsScreen.md) — issues.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/PullRequestDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/PullRequestDetailsScreen.md) — pull requests.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/FastPRDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/FastPRDetailsScreen.md) — fast PRs.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/RefactoringDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/RefactoringDetailsScreen.md) — code volatility (refactoring).
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CodeOwnershipDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/CodeOwnershipDetailsScreen.md) — code ownership.
- [docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/DominantDayDetailsScreen.md](docs/Screens/Tabs/2ndTab/StatisticsScreens/Details/DominantDayDetailsScreen.md) — dominant day of the week.

### Tab 3 — Information

- [docs/Screens/Tabs/3rdTab/InformationScreen.md](docs/Screens/Tabs/3rdTab/InformationScreen.md) — Information screen.
- [docs/Screens/Tabs/3rdTab/SettingsScreen.md](docs/Screens/Tabs/3rdTab/SettingsScreen.md) — app settings.
- [docs/Screens/Tabs/3rdTab/PrivacyPolicy.md](docs/Screens/Tabs/3rdTab/PrivacyPolicy.md) — privacy policy.

#### Profile

- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileScreen.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileScreen.md) — profile.
- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileEditAlert.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileEditAlert.md) — profile edit modal.
- [docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileLogoutConfirmAlert.md](docs/Screens/Tabs/3rdTab/ProfileScreens/ProfileLogoutConfirmAlert.md) — logout confirmation.

#### Feedback

- [docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackScreen.md](docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackScreen.md) — feedback form.
- [docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackThankYouAlert.md](docs/Screens/Tabs/3rdTab/FeedbackScreens/FeedbackThankYouAlert.md) — thank you after submission.

### General Scheme

- [docs/scheme.md](docs/scheme.md) — general scheme/structure (if used in the project).

---

## Repository Structure

The project is a **Kotlin Multiplatform** targeting **Android** and **iOS**.

- `composeApp/` — shared Compose Multiplatform module
  - `src/commonMain/` — shared code for all platforms
  - `src/androidMain/` — Android-specific code
  - `src/iosMain/` — iOS-specific code
  - `src/jvmMain/` — JVM/desktop-specific code (if used)
- `iosApp/` — iOS app / entry point (Swift/Xcode)

---

## Design

- Figma: https://www.figma.com/design/Pac1BvaweGNNmjDnvW2ZdL/IT-CLINIC?node-id=0-1&p=f&t=Se6QVT4BCQxH8jUh-0

---

## 🔐 Developer Configuration

> **Important**: Before first run, you need to set up security configuration!

After cloning the repository:

```bash
# 1. Copy configuration template
cd composeApp/src/commonMain/kotlin/com/spbu/projecttrack/
cp BuildConfig.example.kt BuildConfig.kt

# 2. Generate test token (for local development)
cd Registry/
npm install jsonwebtoken
node generate-test-token.js

# 3. Insert the obtained token into BuildConfig.kt
```

The `BuildConfig.kt` file contains tokens and keys and **is not committed to Git**.

📖 Detailed instructions: [composeApp/SECURITY_README.md](composeApp/SECURITY_README.md)

---

## Build and Run

### Requirements

- Android Studio (for Android)
- Xcode (for iOS)
- JDK (usually installed with Android Studio)

### Android

Build debug version:

- macOS/Linux:
  ```sh
  ./gradlew :composeApp:assembleDebug
  ```

- Windows:
  ```bat
  .\gradlew.bat :composeApp:assembleDebug
  ```

It is easier to run via Android Studio (Run configuration).

### iOS

- Open the `iosApp/` folder in Xcode and run the app.

---

## Troubleshooting

- **Gradle does not build / strange dependencies**: try `./gradlew clean` and rebuild.
- **iOS does not start**: check that Xcode is installed and the correct simulator version is selected.
- **UI changes not visible**: make sure to restart the build/app (sometimes invalidate caches in IDE helps).

## GitHub Actions CI/CD

- Detailed documentation is available in [docs/Development/GITHUB_CICD.md](docs/Development/GITHUB_CICD.md).
- It covers the workflow, GHCR access, `unauthorized` causes, image publishing, and rollback strategy.
