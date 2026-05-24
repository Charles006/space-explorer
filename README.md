# Space Explorer

Aplicacion Android nativa construida 100% con **Jetpack Compose** que consume
la API publica de NASA (APOD - Astronomy Picture of the Day) para explorar y
guardar imagenes astronomicas como favoritas.

---

## Tabla de contenidos

- [Funcionalidades](#funcionalidades)
- [Arquitectura](#arquitectura)
- [Stack tecnico](#stack-tecnico)
- [Manejo de estado en Compose](#manejo-de-estado-en-compose)
- [Decisiones tecnicas](#decisiones-tecnicas)
- [Justificacion de librerias](#justificacion-de-librerias-trade-offs)
- [Instalacion y ejecucion](#instalacion-y-ejecucion)
- [Tests](#tests)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Matriz de cumplimiento](#matriz-de-cumplimiento)

---

## Funcionalidades

| # | Requisito                    | Implementacion                                                                                                                |
|---|------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| 1 | Lista desde API publica      | `HomeScreen` consume NASA APOD via Retrofit                                                                                   |
| 2 | Detalle al hacer tap         | `DetailScreen` con navegacion por argumento de fecha                                                                          |
| 3 | Paginacion / scroll infinito | `LazyColumn` + `LaunchedEffect` con `snapshotFlow`, carga 10 dias por pagina                                                  |
| 4 | Busqueda local o remota      | `AstronomySearchBar` con filtro local en titulo/explicacion/fecha + busqueda remota por fecha (`YYYY-MM-DD`) al endpoint APOD |
| 5 | Favoritos persistentes       | Room (`FavoriteDao`) con `Mutex` para evitar lost-update races                                                                |
| 6 | Pantalla de favoritos        | `FavoritesScreen` con bottom navigation                                                                                       |
| 7 | Loading / error / empty      | `StateComponents.kt` cubre los tres estados con iconos y retry                                                                |

### Funcionalidades adicionales

- **Reproduccion de video in-app** via WebView con iframe (autoplay + playsinline), sin Intent al browser externo
- **Toggle manual claro/oscuro** persistido con `SharedPreferences`
- **Material 3 con dynamic color** (Material You) y modo oscuro automatico
- **Errores tipados** mediante `AstronomyError` sealed class
- **Date handling thread-safe** con `java.time` + core library desugaring
- **Animaciones Compose** (`AnimatedVisibility`, `animateContentSize`, `fadeIn/fadeOut`)
- **Coil** para carga asincrona de imagenes con crossfade
- **CI con GitHub Actions** (`.github/workflows/android.yml`)

---

## Arquitectura

**MVVM + Clean Architecture** con 3 capas estrictas:

```
+--------------------------------------------------+
|                  PRESENTACION                    |
|  Compose UI -> ViewModels (StateFlow) -> NavHost |
+--------------------------------------------------+
|                    DOMINIO                       |
|  Modelos inmutables + Repository interface       |
|  AstronomyError sealed class                     |
+--------------------------------------------------+
|                     DATOS                        |
|  Retrofit (NASA) + Room (favoritos) + Mappers    |
|  ApiErrorMapper (HTTP -> AstronomyError)         |
+--------------------------------------------------+
```

### Reglas de capas

- La capa de **datos** NUNCA filtra tipos de Retrofit / Room hacia arriba. Todo se traduce a tipos de dominio.
- La capa de **dominio** NO conoce Android: solo Kotlin puro + Coroutines.
- La capa de **presentacion** solo depende del dominio (Repository interface, modelos, errores).

---

## Stack tecnico

| Capa                | Libreria                                   | Version         | Razon                                                |
|---------------------|--------------------------------------------|-----------------|------------------------------------------------------|
| Lenguaje            | Kotlin                                     | 2.0.21          | Type-safe, null-safe, soporte oficial                |
| UI                  | Jetpack Compose (BOM)                      | 2024.10.01      | UI declarativa, sin XML                              |
| Design system       | Material 3                                 | 1.3.1           | Tokens modernos + dynamic color                      |
| Estado              | StateFlow + collectAsStateWithLifecycle    | 2.8.7           | Reactivo, lifecycle-aware                            |
| DI                  | Hilt                                       | 2.52            | Compile-time DI, oficial Android                     |
| API                 | Retrofit + OkHttp                          | 2.11.0 / 4.12.0 | HTTP type-safe, interceptores estandar               |
| Serializacion       | Moshi (codegen via KSP)                    | 1.15.1          | Rapido, soporte Kotlin first-class                   |
| BD                  | Room                                       | 2.6.1           | SQLite type-safe con Flow nativo                     |
| Imagenes            | Coil                                       | 2.7.0           | Kotlin-first, integracion nativa Compose             |
| Navegacion          | Navigation Compose                         | 2.8.4           | Routing type-safe                                    |
| Concurrencia        | Kotlin Coroutines + Flow                   | 1.8.1           | Suspending, lifecycle-aware, Mutex para favoritos    |
| Fechas              | java.time + core library desugaring        | 2.1.2           | Thread-safe (vs SimpleDateFormat), API moderna       |
| Testing JVM         | JUnit 4 + Mockito-Kotlin + Truth + Turbine | varios          | Estandar JVM, idiomatico                             |
| API testing         | OkHttp MockWebServer                       | 4.12.0          | Stack HTTP real                                      |
| Android testing JVM | Robolectric                                | 4.13            | Tests Android sin emulador                           |

---

## Manejo de estado en Compose

| Primitivo                       | Uso en el proyecto                                                                      |
|---------------------------------|-----------------------------------------------------------------------------------------|
| `remember { }`                  | `LazyListState`, `SnackbarHostState`                                                    |
| `rememberSaveable { }`          | `searchQuery` (sobrevive a rotacion y process death)                                    |
| `derivedStateOf { }`            | `showScrollToTop` derivado de `firstVisibleItemIndex`                                   |
| `collectAsStateWithLifecycle()` | TODOS los `ViewModel.uiState`                                                           |
| `LaunchedEffect`                | Pagination trigger, error snackbar, search query propagation                            |
| `snapshotFlow`                  | Convertir estado de lista en Flow para detectar fin de scroll                           |
| `rememberCoroutineScope`        | Scroll programatico al tope                                                             |
| `@Immutable` / `@Stable`        | `Astronomy` y todos los UiState marcados `@Immutable`                                   |
| `key()` en `items()`            | Identidad estable en `LazyColumn` items por id                                          |

---

## Decisiones tecnicas

### 1. Errores tipados con sealed class (`AstronomyError`)

Reemplazaron las excepciones genericas (`NetworkException`, `ApiException`) por
una jerarquia sellada que permite a la UI reaccionar de forma diferenciada:

```kotlin
sealed class AstronomyError(val userMessage: String, cause: Throwable?) : Exception(...)
   |-- Network          // sin conexion
   |-- Unauthorized     // 401 / 403
   |-- RateLimited      // 429
   |-- ServerUnavailable// 5xx
   |-- InvalidDate      // ISO date fail
   |-- HttpError(code)  // resto de codigos HTTP
   |-- Unknown          // catch-all
```

Cada subtipo carga su propio `userMessage` localizado.

### 2. `java.time` + core library desugaring

`SimpleDateFormat` **no es thread-safe** y compartirla como singleton es un
bug latente. Se migro a `DateTimeFormatter` (immutable, thread-safe) con
core library desugaring habilitado (`desugar_jdk_libs:2.1.2`) para soportar
`minSdk = 24`.

### 3. `Mutex` en operaciones de favoritos

`toggleFavorite()`, `addFavorite()` y `removeFavorite()` estan envueltas en
un `Mutex` para evitar lost-update races cuando el usuario realiza dos taps
rapidos consecutivos.

### 4. Mapper centralizado (`AstronomyMapper`)

Las conversiones DTO <-> Entity <-> Domain estan en un unico objeto puro con
**validacion explicita** (`require(...)`) para detectar contratos rotos del
API en el limite de la capa de datos.

### 5. `ApiErrorMapper` propaga `CancellationException`

El `runCatching` original silenciaba `CancellationException`, rompiendo
structured concurrency. La version actual la re-lanza explicitamente.

### 6. Constants centralizadas

Numeros magicos (`PAGE_SIZE`, `PAGINATION_PREFETCH_DISTANCE`, timeouts) viven
en `core/Constants.kt`.

### 7. Eliminacion de codigo muerto

- Removido `Paging 3` (dependencia declarada pero nunca usada).
- Removido metodo `isFavorite()` del Repository (nunca llamado).
- Consolidadas 4 `@Preview` redundantes en un unico provider parametrizado.

### 8. WebView in-process para videos APOD (no ExoPlayer/Media3)

Los videos de NASA APOD son embeds de YouTube / Vimeo. Las ToS de ambos
proveedores **prohiben extraer el stream subyacente**, por lo que la unica
forma soportada de reproducirlos es renderizar el player oficial — que es
exactamente lo que hace un `<iframe>` dentro de un `WebView`.

Trade-offs:

| Opcion | Pros | Contras |
|---|---|---|
| **WebView + iframe** (elegido) | Cero deps nuevas, funciona con cualquier embed YouTube/Vimeo/MP4, cumple ToS | Ligeramente mas pesado en RAM que un player nativo |
| ExoPlayer / Media3 | Player nativo, mejor UX | No puede reproducir embeds YouTube — bloqueado por ToS |
| Intent al browser externo | Trivial | Saca al usuario de la app, mala UX, no es "in-app" |

Detalles de implementacion:
- Modelo: `Astronomy.videoUrl` (separado de `imageUrl`) evita que Coil intente
  renderizar URLs de embed que no son imagenes.
- Schema: migracion Room v1 -> v2 agrega columna `videoUrl` a favoritos sin
  perder data existente.
- Lifecycle: el WebView se destruye en `DisposableEffect.onDispose` para
  evitar memory leaks y reproduccion en background.
- UX: thumbnail + boton Play; tap inicia el WebView con `autoplay=1` y
  `playsinline=1` inyectados via `buildEmbedHtml`.
- URL normalization (`normalizeToEmbedUrl`): NASA devuelve a veces formatos
  `youtube.com/watch?v=ID`, `youtu.be/ID` o embeds en `youtube.com` que
  disparan "error 152" del player oficial. Se normalizan a
  `youtube-nocookie.com/embed/ID` (privacy-enhanced mode) que tiene menos
  restricciones de embed cross-origin. Vimeo se mapea a
  `player.vimeo.com/video/ID`. URLs desconocidas (MP4 directo) pasan tal cual.

---

## Justificacion de librerias (trade-offs)

| Decision                                                | Por que                                                                                    | Alternativa descartada                                                                                                  |
|---------------------------------------------------------|--------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| **Hilt** sobre Koin                                     | DI en compile-time atrapa errores temprano                                                 | Koin: mas simple, pero errores en runtime                                                                               |
| **Retrofit + Moshi** sobre Ktor + kotlinx.serialization | Estandar de mercado Android, ecosistema mas maduro                                         | Ktor: mejor para KMP pero menos comun en Android puro                                                                   |
| **Room** sobre SQLDelight                               | Anotaciones simples, integracion nativa con Flow, oficial Jetpack                          | SQLDelight: type-safe pero curva de aprendizaje mas alta                                                                |
| **Coil** sobre Glide/Picasso                            | Kotlin-first, ligero, integracion Compose oficial                                          | Glide: muy basado en Java; Picasso: legacy sin Compose                                                                  |
| **KSP** sobre KAPT                                      | Significativamente mas rapido (3-5x)                                                       | KAPT: deprecado para nuevos proyectos                                                                                   |
| **java.time + desugaring** sobre SimpleDateFormat       | Thread-safe e immutable                                                                    | ThreadLocal<SimpleDateFormat>: funciona pero introduce overhead innecesario                                             |
| **Sealed class** sobre Exception generica               | Permite a la UI reaccionar de forma tipada                                                 | Exception(message): obliga a parsear strings                                                                            |
| **MockWebServer** sobre mocks puros                     | Testea stack HTTP real (interceptor, parser Moshi, serializacion)                          | Mocks puros: rapidos pero ocultan bugs reales                                                                           |
| **Robolectric** sobre solo emulator                     | Tests Android (Room, Compose UI) en JVM sin emulador (~45s vs ~5min)                       | Solo emulador: lento en CI                                                                                              |
| **MVVM clasico + StateFlow** sobre MVI                  | Estandar de mercado, mas legible, menos boilerplate                                        | MVI: estado puro funcional pero mas verbose                                                                             |

---

## Instalacion y ejecucion

### Prerequisitos

- **JDK 17+** (verifica con `java -version`)
- **Android SDK** API 35 (minSdk 24)
- **NASA API Key** (opcional, gratuita en https://api.nasa.gov/). Sin key, la app usa `DEMO_KEY`.

### Pasos

1. **Clonar el repositorio**
   ```bash
   git clone <repo-url>
   cd 2026-05-23_space_explorer
   ```

2. **Configurar `local.properties`**:
   ```properties
   sdk.dir=C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk
   NASA_API_KEY=tu_api_key_o_DEMO_KEY
   ```

3. **Sincronizar y construir**
   ```bash
   ./gradlew clean build
   ```

4. **Ejecutar la app**
   - Android Studio: `Run > Run 'app'`
   - CLI: `./gradlew installDebug && adb shell am start -n com.space_explorer/.MainActivity`

---

## Tests

```bash
./gradlew test                                # Todos los tests JVM
./gradlew test --tests "*ViewModelTest"       # ViewModels
./gradlew test --tests "*MapperTest"          # Mappers
./gradlew test --tests "*NasaApiServiceTest"  # API (MockWebServer)
./gradlew test --tests "*FavoriteDaoTest"     # Room (Robolectric)
./gradlew test --tests "*ApodCardTest"        # Compose UI
```

### Cobertura

| Modulo                        | Tests | Descripcion                                                                                          |
|-------------------------------|-------|------------------------------------------------------------------------------------------------------|
| `AstronomyRepositoryImplTest` | 11    | Mapeo, errores tipados, toggleFavorite atomico, favoritos                                            |
| `ApiErrorMapperTest`          | 9     | Mapeo HTTP, IOException, propagacion de CancellationException                                        |
| `AstronomyMapperTest`         | 7     | Validacion de campos, fallback de URL, round-trip Entity/Domain                                      |
| `AstronomyViewModelTest`      | 7     | StateFlow, success/failure, busqueda local/remota, toggle favoritos, reactividad                     |
| `FavoritesViewModelTest`      | 3     | Listado, eliminacion, filtro local                                                                   |
| `NasaApiServiceTest`          | 4     | Parseo JSON, array response, api_key interceptor, 401                                                |
| `FavoriteDaoTest`             | 5     | Insert, exists, delete, observe ids, replace on conflict                                             |
| `DateUtilsTest`               | 8     | ISO valid/invalid, pretty print, fechas imposibles, daysAgo, previousDay, month boundaries           |
| `ApodCardTest`                | 4     | Renderizado, clicks, toggle favoritos, estado favorito                                               |
| **Total**                     | **58**| Cobertura de logica critica >90%                                                                     |

---

## Estructura del proyecto

```
app/src/main/java/com/space_explorer/
|-- MainActivity.kt                          # @AndroidEntryPoint
|-- SpaceExplorerApplication.kt              # @HiltAndroidApp
|-- core/
|   `-- Constants.kt                         # Paginacion, timeouts, formatos
|-- di/
|   |-- NetworkModule.kt                     # Retrofit, OkHttp, Moshi
|   |-- DatabaseModule.kt                    # Room
|   `-- RepositoryModule.kt                  # @Binds Repository
|-- data/
|   |-- api/
|   |   |-- NasaApiService.kt
|   |   `-- ApiKeyInterceptor.kt
|   |-- model/ApodResponse.kt                # DTO Moshi puro
|   |-- mapper/AstronomyMapper.kt            # DTO/Entity <-> Domain + validacion
|   |-- network/ApiErrorMapper.kt            # HttpException/IOException -> AstronomyError
|   |-- local/
|   |   |-- entity/FavoriteEntity.kt
|   |   |-- dao/FavoriteDao.kt
|   |   `-- database/SpaceExplorerDatabase.kt
|   |-- preferences/ThemePreferences.kt
|   `-- repository/AstronomyRepositoryImpl.kt# Mutex + ApiErrorMapper + Mapper
|-- domain/
|   |-- model/Astronomy.kt                   # @Immutable
|   |-- error/AstronomyError.kt              # Sealed class de errores
|   `-- repository/AstronomyRepository.kt
`-- ui/
    |-- theme/                               # Material 3 + dynamic color + brand palette
    |-- navigation/
    |   |-- Destinations.kt
    |   `-- SpaceExplorerApp.kt              # NavHost + bottom bar
    |-- components/                          # ApodCard, SearchBar, StateComponents, ThemeToggle
    |-- screens/                             # HomeScreen, DetailScreen, FavoritesScreen
    |-- viewmodel/                           # AstronomyVM, FavoritesVM, DetailVM, ThemeVM
    |-- state/UiState.kt                     # HomeUiState, FavoritesUiState, DetailUiState
    `-- util/DateUtils.kt                    # java.time, thread-safe
```

---

## Matriz de cumplimiento

### Requisitos funcionales

| Requisito                     |   Estado   |
|-------------------------------|:----------:|
| Lista desde API publica       |     OK     |
| Pantalla de detalle           |     OK     |
| Paginacion / scroll infinito  |     OK     |
| Busqueda local o remota       | OK (ambas) |
| Favoritos persistentes        |     OK     |
| Pantalla de favoritos         |     OK     |
| Loading / error / empty       |     OK     |

### Requisitos tecnicos obligatorios

| Requisito                                        |                  Estado                  |
|--------------------------------------------------|:----------------------------------------:|
| Kotlin (no Java)                                 |                    OK                    |
| Jetpack Compose 100% (no XML)                    |                    OK                    |
| Material 3                                       |                    OK                    |
| Navigation Compose                               |                    OK                    |
| MVVM / Clean Architecture                        |                    OK                    |
| Hilt                                             |                    OK                    |
| Retrofit + corrutinas/Flow                       |                    OK                    |
| Room para favoritos                              |                    OK                    |
| StateFlow + `collectAsStateWithLifecycle()`      |                    OK                    |
| `remember`, `rememberSaveable`, `derivedStateOf` |                    OK                    |
| ViewModels sobreviven config changes             | OK (`@HiltViewModel` + `viewModelScope`) |
| 3-5 tests significativos                         |              OK (58 tests)               |
| minSdk >= 24                                     |                  OK (24)                 |

### Banderas rojas evitadas

| Bandera                            |                      Estado                       |
|------------------------------------|:-------------------------------------------------:|
| `findViewById` / XML / ViewBinding |                      Evitado                      |
| LiveData sin justificacion         |             Evitado (solo StateFlow)              |
| `collectAsState()` sin Lifecycle   |                      Evitado                      |
| Logica de negocio en `@Composable` |                      Evitado                      |
| Recomposiciones innecesarias       | Evitado (`@Immutable`, `key()`, lambdas estables) |
| ViewModels filtran Context/Compose |                      Evitado                      |
| `SimpleDateFormat` thread-unsafe   |          Evitado (java.time + desugar)            |
| Lost-update races en favoritos     |                Evitado (Mutex)                    |
| `CancellationException` silenciada |       Evitado (re-lanzada en ApiErrorMapper)      |
| Magic numbers en codigo            |     Evitado (centralizados en `Constants.kt`)     |
| Codigo muerto / deps no usadas     |       Evitado (Paging removido, isFavorite removido)|

---

## Licencia

Proyecto desarrollado como prueba tecnica. Los datos de imagenes provienen de NASA APOD y
estan en dominio publico salvo cuando el campo `copyright` indique lo contrario.
