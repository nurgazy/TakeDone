Это проект Kotlin Multiplatform, ориентированный на Android и iOS.

* [/iosApp](./iosApp/iosApp) содержит приложение для iOS. Даже если ваш пользовательский интерфейс используется совместно с помощью Compose Multiplatform, эта точка входа необходима для iOS-приложения. Также сюда следует добавлять код SwiftUI для вашего проекта.

* [/shared](./shared/src) предназначен для кода, который используется совместно между вашими приложениями Compose Multiplatform.
  В этой директории содержится несколько подпапок:
  - [commonMain](./shared/src/commonMain/kotlin) — для кода, общего для всех целевых платформ.
  - Другие папки предназначены для кода Kotlin, который компилируется только для платформы, указанной в названии папки.
    Например, если вы хотите использовать Apple CoreCrypto для iOS-части вашего приложения на Kotlin,
    подходящим местом для таких вызовов будет папка [iosMain](./shared/src/iosMain/kotlin).
    Аналогично, если вы хотите отредактировать часть, специфичную для Desktop (JVM), подходящим местом будет папка [jvmMain](./shared/src/jvmMain/kotlin).

### Запуск приложений

Используйте конфигурации запуска на панели инструментов вашей IDE. Вы также можете использовать следующие команды и параметры:

- Приложение Android: `./gradlew :androidApp:assembleDebug`
- Приложение iOS: откройте директорию [/iosApp](./iosApp) в Xcode и запустите приложение оттуда.

### Запуск тестов

Используйте кнопку запуска на боковой панели редактора вашей IDE или запускайте тесты с помощью задач Gradle:

- Тесты Android: `./gradlew :shared:testAndroidHostTest`
- Тесты iOS: `./gradlew :shared:iosSimulatorArm64Test`

---

Узнайте больше о [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
