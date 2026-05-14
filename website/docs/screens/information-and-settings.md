---
title: Information, Settings and Feedback Screens
---

# Information, Settings and Feedback Screens

## Information Screen

### Назначение

Это вход в personal area пользователя. Экран связывает profile, settings, privacy policy и feedback flows.

### Дизайн и композиция

- верхний title `Информация`;
- profile card с аватаром, именем, email и краткой project summary;
- list rows для переходов в `Настройки`, `Политика конфиденциальности` и `Обратная связь`;
- простой list-based layout без перегруженных действий на первом экране.

### Состояния

- loading профиля;
- populated state с данными пользователя;
- частично доступный state, если project summary отсутствует;
- error state, если профиль не удалось загрузить.

### Данные

Экрану нужны:

- user profile;
- current project summary для строки проекта в карточке.

### Запросы

- `GET /user/profile`
- связанные данные проекта могут подтягиваться из project-related flows и локального состояния приложения

## Profile Screen

### Назначение

Показывает подробные данные пользователя, состав команды и текущий проект, а также дает доступ к logout и редактированию профиля.

### Дизайн и композиция

- header с back navigation;
- profile card с avatar, full name, email, phone;
- edit action для редактирования personal data;
- team block со списком участников;
- project block с текущим проектом;
- logout action как отдельная явная строка.

### Состояния

- loading;
- ready state;
- empty team / no current project;
- logout confirmation dialog;
- edit profile modal.

### Запросы

- `GET /user/profile`
- `PUT /profile/personal`
- `PUT /profile/account` для account-related изменений, если flow разделен по данным

## Edit Profile Modal

### Назначение

Позволяет менять персональные данные пользователя без выхода из profile flow.

### Дизайн

- modal surface поверх profile screen;
- поля `Имя`, `Фамилия`, `Отчество`, `Почта`, `Номер телефона`;
- настройка уведомлений;
- primary action `Сохранить`.

### Состояния формы

- initial values из текущего профиля;
- local dirty state;
- validation errors;
- saving state;
- dismiss without save.

### Запросы

- `PUT /profile/personal`

## Settings Screen

### Назначение

Экран отвечает за пользовательские app-level preferences, а не за бизнес-данные проекта.

### Дизайн и композиция

- простой list layout;
- rows для выбора языка интерфейса;
- rows для выбора темы;
- вложенные chooser/dialog flows для конкретных настроек.

### Локальные данные

Настройки живут в локальном persistent store клиента и применяются без отдельного backend round-trip, если речь идет о theme/language.

### Запросы

Для language/theme backend endpoint не обязателен. Если экран расширяется account settings, возможны:

- `PUT /profile/personal`
- `PUT /profile/account`

## Privacy Policy Screen

### Назначение

Отображает policy text в отдельном навигационном узле без смешивания с editable settings.

### Дизайн

- header с back navigation;
- scrollable text content;
- акцент на читаемости и длинном тексте, а не на действиях.

### Данные

Контент может быть встроен в клиент или приходить из заранее подготовленного источника конфигурации. Для пользователя это read-only flow.

## Feedback Screen

### Назначение

Собирает свободный текст с проблемами, пожеланиями и предложениями.

### Дизайн и композиция

- header и explanatory copy;
- large multiline input;
- primary send button;
- success state или thank-you confirmation после отправки.

### Локальные состояния

- draft текста;
- empty-state validation;
- `isSending`;
- success/failure result;
- защита от повторной отправки во время in-flight запроса.

### Сетевое поведение

Feedback flow не опирается на основной backend endpoint. Клиент использует mail sending integration для отправки сообщения через настроенный SMTP/mail channel.
