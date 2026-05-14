---
title: Backend Gaps
---

# Backend Gaps

## Что здесь считается gap

Gap — это место, где mobile client уже реализует поведение, но backend contract для него либо неполный, либо неудобный, либо отсутствует в явном виде.

## Наиболее заметные gap'ы

- ranking по произвольным метрикам и диапазонам во многом считается на клиенте;
- detail screens статистики собираются из snapshot payload, а не из dedicated detail endpoints;
- часть scoring logic основана на client-side эвристиках;
- feedback пока не вынесен в безопасный backend endpoint.

## Практическое правило

Если вы меняете ranking/stats код и вынуждены добавить новый workaround, его нужно отразить здесь, а не только в комментарии внутри repository.

## Практическое использование

Если появляется новый client-side workaround или временный backend gap, отражайте его в этом разделе одновременно с изменением кода.
