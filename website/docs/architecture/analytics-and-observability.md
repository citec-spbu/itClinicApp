---
title: Analytics and Observability
---

# Analytics and Observability

## Назначение

Клиент собирает product analytics для основных пользовательских сценариев и экранов статистики. Цель не в raw event spam, а в измеримом понимании того:

- какие экраны реально открывают;
- какие metric blocks просматривают;
- где пользователь взаимодействует со статистикой и фильтрами;
- как auth lifecycle влияет на identity и session tracking.

## Основные компоненты

- `AnalyticsTracker` в `commonMain` задает общий контракт;
- `CompositeAnalyticsTracker` fan-out'ит события на несколько провайдеров;
- debug/dev слой может логировать события локально;
- production providers включают PostHog и Firebase integrations.

## Event model

Событие содержит:

- имя;
- timestamp;
- session id;
- optional user id;
- properties map с экраном, блоком, типом действия и дополнительными параметрами.

## Что реально трекается

- `screen_viewed`;
- visibility/view events для metric blocks;
- focus events на ключевых статистических блоках;
- tap/expand interactions;
- изменения настроек статистики только при фактическом изменении состава или порядка блоков.

## Защита от шума

Аналитика не должна стрелять на каждый scroll tick. Для этого используются:

- thresholds по `visibleRatio`;
- минимальная длительность видимости блока;
- debounce/throttle для interaction-heavy flows;
- агрегация focus time по нескольким появлениям блока за один экранный сеанс.

## Identity lifecycle

- после логина пользователь идентифицируется безопасным application-level id;
- при logout identity и session-sensitive state сбрасываются;
- при уходе приложения в background выполняется flush;
- при возврате в foreground session может быть обновлена.

## Что важно не нарушить при развитии

- не дублировать screen events платформенными auto-track механизмами;
- не отправлять чувствительные данные пользователя в event payload;
- не подвязывать analytics к частым recomposition в Compose;
- не добавлять события, которые нельзя потом интерпретировать продуктово.
