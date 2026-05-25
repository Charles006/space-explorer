# Space Explorer

[![CI](https://github.com/Charles006/space-explorer/actions/workflows/android.yml/badge.svg)](https://github.com/Charles006/space-explorer/actions/workflows/android.yml)
[![codecov](https://codecov.io/gh/Charles006/space-explorer/branch/main/graph/badge.svg)](https://codecov.io/gh/Charles006/space-explorer)

Android app (Jetpack Compose) que lista las imagenes y videos de NASA APOD
(Astronomy Picture of the Day), permite buscar por fecha, marcar favoritos y
reproducir los videos dentro de la app via WebView. Soporta espanol e ingles
por locale del sistema.

## Stack tecnico

| Capa                | Libreria                                   | Version         |
|---------------------|--------------------------------------------|-----------------|
| Lenguaje            | Kotlin                                     | 2.0.21          |
| UI                  | Jetpack Compose (BOM)                      | 2024.10.01      |
| Design system       | Material 3                                 | 1.3.1           |
| Estado              | StateFlow + collectAsStateWithLifecycle    | 2.8.7           |
| DI                  | Hilt                                       | 2.52            |
| API                 | Retrofit + OkHttp                          | 2.11.0 / 4.12.0 |
| Serializacion       | Moshi (codegen via KSP)                    | 1.15.1          |
| BD                  | Room (schema v2, MIGRATION_1_2)            | 2.6.1           |
| Imagenes            | Coil                                       | 2.7.0           |
| Video in-app        | Android WebView + iframe (sin deps extra)  | sistema         |
| Navegacion          | Navigation Compose                         | 2.8.4           |
| Concurrencia        | Coroutines + Flow, Mutex para favoritos    | 1.8.1           |
| Fechas              | java.time + core library desugaring        | 2.1.2           |
| Testing JVM         | JUnit 4 + Mockito-Kotlin + Truth + Turbine | varios          |
| API testing         | OkHttp MockWebServer                       | 4.12.0          |
| Android testing JVM | Robolectric                                | 4.13            |
| Arquitectura tests  | Konsist                                    | 0.17.3          |
| Coverage            | Kover                                      | 0.8.3           |
| Static analysis     | detekt + Spotless (ktlint)                 | 1.23.7 / 6.25.0 |

## Arquitectura

MVVM + Clean Architecture en tres capas:

```
+--------------------------------------------------+
|                  PRESENTACION                    |
|  Compose UI -> ViewModels (StateFlow) -> NavHost |
+--------------------------------------------------+
|                    DOMINIO                       |
|  Modelos inmutables + Repository interface       |
|  AstronomyError sealed class (tipos de error)    |
+--------------------------------------------------+
|                     DATOS                        |
|  Retrofit (NASA) + Room (favoritos) + Mappers    |
|  ApiErrorMapper (HTTP -> AstronomyError)         |
+--------------------------------------------------+
```

Konsist enforza la ubicacion de ViewModels, Repository interface, RepositoryImpl
y Mappers. Si alguien mete un `XxxViewModel` fuera de `ui.viewmodel`, el test
de arquitectura falla.

## Correr local

Requisitos: JDK 17, Android SDK API 35 (minSdk 24).

```
git clone https://github.com/Charles006/space-explorer
cd space-explorer
./gradlew assembleDebug
./gradlew installDebug
```

`local.properties` viaja con el repo: trae una NASA API key publica y un
`sdk.dir` con el path Windows por defecto. Si tu SDK vive en otro lado,
edita esa linea o exporta `ANDROID_HOME`. Si abres el proyecto en Android
Studio, el IDE regenera `sdk.dir` solo en el primer sync.

## Idioma

Strings en `app/src/main/res/values/strings.xml` (espanol, default) y
`values-en/strings.xml` (ingles). El locale se elige por configuracion
del sistema; en runtime no se cambia.

## Tools

Todo lo siguiente corre en JVM, no necesita emulador.

| Comando                                | Para que                                     |
|----------------------------------------|----------------------------------------------|
| `./gradlew testDebugUnitTest`          | toda la suite unitaria                       |
| `./gradlew spotlessApply`              | auto-formato                                 |
| `./gradlew spotlessCheck`              | falla si algo esta sin formatear             |
| `./gradlew detekt`                     | reporte en `app/build/reports/detekt/`       |
| `./gradlew detektBaseline`             | regenera el baseline tras un refactor        |
| `./gradlew koverHtmlReportDebug`       | reporte HTML en app/build/reports/kover/html |
| `./gradlew koverXmlReportDebug`        | XML para Codecov                             |
| `./gradlew koverVerifyDebug`           | falla si la cobertura baja del threshold     |

### Compose Compiler Metrics

Reporte de estabilidad / skippability de Composables. Esta apagado por
defecto; activalo via property cuando lo quieras inspeccionar:

```
./gradlew :app:compileDebugKotlin -PcomposeMetricsEnabled=true --rerun-tasks
```

Output en `app/build/compose_compiler/`: `app_debug-classes.txt`,
`app_debug-composables.txt`, `app_debug-composables.csv`,
`app_debug-module.json`.

### Konsist (architecture tests)

Corren con el resto de los unit tests:

```
./gradlew testDebugUnitTest --tests "*ArchitectureTest*"
```

`test` solo es un agregador y no acepta `--tests`; hay que usar el task
del variant (`testDebugUnitTest`).

## Estructura del proyecto

```
app/src/main/java/com/space_explorer/
|-- MainActivity.kt                          # @AndroidEntryPoint
|-- SpaceExplorerApplication.kt              # @HiltAndroidApp
|-- core/Constants.kt                        # paginacion, timeouts, aspect ratios
|-- di/                                      # NetworkModule, DatabaseModule, RepositoryModule
|-- data/
|   |-- api/                                 # NasaApiService, ApiKeyInterceptor
|   |-- model/ApodResponse.kt                # DTO Moshi
|   |-- mapper/AstronomyMapper.kt            # DTO/Entity <-> Domain
|   |-- network/ApiErrorMapper.kt            # HttpException/IOException -> AstronomyError
|   |-- local/                               # FavoriteEntity, FavoriteDao, SpaceExplorerDatabase
|   |-- preferences/ThemePreferences.kt
|   `-- repository/AstronomyRepositoryImpl.kt# Mutex + ApiErrorMapper + Mapper
|-- domain/
|   |-- model/Astronomy.kt                   # @Immutable
|   |-- error/AstronomyError.kt              # sealed class de errores
|   `-- repository/AstronomyRepository.kt
`-- ui/
    |-- theme/                               # Material 3 + dynamic color
    |-- navigation/                          # Destinations, SpaceExplorerApp (NavHost)
    |-- components/                          # ApodCard, SearchBar, StateComponents, ThemeToggle, EmbeddedVideoPlayer
    |-- screens/                             # HomeScreen, DetailScreen, FavoritesScreen
    |-- viewmodel/                           # AstronomyVM, FavoritesVM, DetailVM, ThemeVM
    |-- state/UiState.kt
    `-- util/DateUtils.kt                    # java.time, thread-safe
```

## Notas

- Paginacion custom de 10 dias por pagina hacia atras. NASA APOD no expone
  cursor, solo rango de fechas.
- Videos: NASA devuelve embeds YouTube/Vimeo o MP4 directos. WebView elige
  entre `<iframe>` (YouTube/Vimeo, normalizado a `youtube-nocookie.com`) y
  `<video>` (MP4) segun la URL.
- CI: `.github/workflows/android.yml` corre tests, Spotless, detekt,
  Kover (con threshold y upload a Codecov) y `assembleDebug` en cada push.
