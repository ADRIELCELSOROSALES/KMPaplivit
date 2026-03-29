This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…















Aplivit — Documento de contexto técnico v
Visión general
Aplivit es una aplicación móvil multiplataforma orientada a enseñar lectura básica desde sílabas hasta la formación de palabras, dirigida a usuarios con bajo nivel de alfabetización. La experiencia está completamente guiada por voz, eliminando la dependencia del texto escrito. No requiere registro ni conexión a internet para funcionar, aunque aprovecha la conectividad cuando está disponible para mejorar el reconocimiento de voz.

Plataformas objetivo
El proyecto apunta a Android e iOS desde una única base de código. La UI es completamente compartida mediante Compose Multiplatform. Durante el desarrollo inicial se trabaja exclusivamente sobre Android; el target iOS está configurado en el proyecto desde el día uno pero no se compila hasta contar con un entorno Mac con Xcode. Esto garantiza que commonMain nunca necesite refactorizarse cuando se active iOS.

Stack tecnológico
Responsabilidad
Solución
UI compartida
Compose Multiplatform 1.7.0
Lenguaje
Kotlin 2.0.21
Estado
ViewModel (androidx.lifecycle) + StateFlow
Navegación
Navigation Compose (Jetbrains)
DI
Koin 3.5.6
Persistencia
multiplatform-settings 1.1.1
Contenido
levels.json + kotlinx.serialization
TTS
expect/actual → Android TextToSpeech / iOS AVSpeechSynthesizer
STT
expect/actual → Android SpeechRecognizer / iOS SFSpeechRecognizer
Conectividad
expect/actual → Android ConnectivityManager / iOS NWPathMonitor
Coroutines
kotlinx.coroutines 1.9.0


Arquitectura
Clean Architecture en tres capas, todas dentro de commonMain excepto los actual de plataforma.
Capa de dominio contiene las entidades del negocio (Level, Syllable, UserProgress, GameResult), los casos de uso (GetLevelsUseCase, CompleteGameUseCase, ValidatePronunciationUseCase, UnlockNextLevelUseCase) y los puertos o interfaces (ProgressRepository, SpeechSynthesizer, SpeechRecognizer, ConnectivityChecker). Esta capa no tiene dependencias de ninguna librería externa ni de plataforma.
Capa de infraestructura contiene las implementaciones concretas de los puertos: SettingsProgressRepository para persistencia usando multiplatform-settings, LevelsLoader para deserializar levels.json con kotlinx.serialization, y los expect de SpeechSynthesizer, SpeechRecognizer y ConnectivityChecker. Los actual de estas tres clases viven en androidMain e iosMain.
Capa de presentación contiene las pantallas Compose, los ViewModels y los componentes reutilizables. Los ViewModels dependen únicamente de los casos de uso, nunca de infraestructura directamente.

Estructura de módulos
Aplivit/
├── composeApp/
│   ├── commonMain/
│   │   ├── kotlin/com/aplivit/
│   │   │   ├── App.kt
│   │   │   ├── core/
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Level.kt
│   │   │   │   │   │   ├── Syllable.kt
│   │   │   │   │   │   ├── UserProgress.kt
│   │   │   │   │   │   └── GameResult.kt
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── GetLevelsUseCase.kt
│   │   │   │   │       ├── CompleteGameUseCase.kt
│   │   │   │   │       ├── ValidatePronunciationUseCase.kt
│   │   │   │   │       └── UnlockNextLevelUseCase.kt
│   │   │   │   └── port/
│   │   │   │       ├── ProgressRepository.kt
│   │   │   │       ├── SpeechSynthesizer.kt
│   │   │   │       ├── SpeechRecognizer.kt
│   │   │   │       └── ConnectivityChecker.kt
│   │   │   ├── infrastructure/
│   │   │   │   ├── content/
│   │   │   │   │   └── LevelsLoader.kt
│   │   │   │   ├── speech/
│   │   │   │   │   ├── SpeechSynthesizer.kt   ← expect
│   │   │   │   │   ├── SpeechRecognizer.kt    ← expect
│   │   │   │   │   └── ConnectivityChecker.kt ← expect
│   │   │   │   └── storage/
│   │   │   │       └── SettingsProgressRepository.kt
│   │   │   └── presentation/
│   │   │       ├── navigation/
│   │   │       │   └── AppNavigation.kt
│   │   │       ├── component/
│   │   │       │   ├── SyllableCard.kt
│   │   │       │   ├── AudioButton.kt
│   │   │       │   └── LevelCard.kt
│   │   │       └── screen/
│   │   │           ├── home/
│   │   │           │   ├── HomeScreen.kt
│   │   │           │   └── HomeViewModel.kt
│   │   │           ├── level/
│   │   │           │   ├── LevelScreen.kt
│   │   │           │   └── LevelViewModel.kt
│   │   │           └── game/
│   │   │               ├── DragDropGameScreen.kt
│   │   │               ├── SelectionGameScreen.kt
│   │   │               ├── RepeatGameScreen.kt
│   │   │               └── GameViewModel.kt
│   │   └── resources/
│   │       └── levels.json
│   ├── androidMain/
│   │   └── kotlin/com/aplivit/
│   │       └── speech/
│   │           ├── SpeechSynthesizer.android.kt
│   │           ├── SpeechRecognizer.android.kt
│   │           └── ConnectivityChecker.android.kt
│   └── iosMain/
│       └── kotlin/com/aplivit/
│           └── speech/
│               ├── SpeechSynthesizer.ios.kt
│               ├── SpeechRecognizer.ios.kt
│               └── ConnectivityChecker.ios.kt
├── androidApp/
│   └── MainActivity.kt
└── iosApp/
    └── (generado por wizard, no tocar)

Flujo de la aplicación
Al iniciar, la app carga automáticamente el perfil local. Si no existe, crea uno desde el nivel 1. Si existe, retoma desde donde el usuario lo dejó. No hay pantalla de login en ningún caso.
La pantalla Home muestra la lista de niveles con estado bloqueado o desbloqueado. Al entrar reproduce automáticamente un audio de bienvenida con instrucciones. El usuario puede continuar el nivel actual o repetir niveles anteriores.
Cada nivel sigue una secuencia fija: instrucción en voz → presentación de sílabas tocables → Juego 1 → Juego 2 → Juego 3 → guardar progreso → desbloquear siguiente nivel.

Los tres juegos
Juego 1 — Drag & Drop: el usuario arrastra sílabas para formar una palabra. Validación inmediata con feedback por voz.
Juego 2 — Selección: se reproduce una sílaba en audio y el usuario debe tocar la opción correcta entre tres. Feedback por voz.
Juego 3 — Repetición con STT y fallback: el sistema detecta conectividad al iniciar el juego y elige el modo automáticamente.
¿Hay internet?
    ├── SÍ  → STT real, valida pronunciación contra sílaba esperada
    └── NO  → Modo amplitud, detecta que hubo sonido y marca como correcto
En ambos casos el juego se puede completar y el progreso nunca se bloquea.

Modelo de datos
kotlin
data class Level(
    val id: Int,
    val syllables: List<Syllable>,
    val word: String,
    val instruction: String
)

data class Syllable(
    val text: String
)

data class UserProgress(
    val currentLevel: Int,
    val completedLevels: Set<Int>,
    val totalErrors: Int
)

sealed class GameResult {
    object Success : GameResult()
    data class Failure(val attempts: Int) : GameResult()
}

Reconocimiento de voz — contratos
kotlin
enum class RecognitionMode { STT, AMPLITUDE }

sealed class RecognitionResult {
    data class Transcription(val text: String) : RecognitionResult()
    object SoundDetected : RecognitionResult()
    object NoSound : RecognitionResult()
    object Error : RecognitionResult()
}

interface SpeechRecognizer {
    val mode: RecognitionMode
    fun startListening(expected: String, onResult: (RecognitionResult) -> Unit)
    fun stopListening()
}

Persistencia
SettingsProgressRepository usa multiplatform-settings que internamente usa SharedPreferences en Android y NSUserDefaults en iOS. No requiere configuración adicional por plataforma. Los datos guardados son: nivel actual, set de niveles completados serializado como string separado por comas, y contador total de errores.

Contenido de niveles
levels.json vive en commonMain/resources y se carga una única vez al iniciar la app vía LevelsLoader. El MVP incluye 15 niveles con una palabra por nivel. Las primeras familias silábicas son MA-ME-MI, PA-PE-PI, SA-SE-SI, LA-LE-LI, CA-CO-CU, TA-TE-TI.

Inyección de dependencias
Koin se inicializa en androidMain dentro de MainActivity y en iosMain dentro del helper de inicialización llamado desde Swift. El módulo de DI vive en commonMain y declara todas las dependencias compartidas. Los actual se proveen como parámetros al módulo usando expect/actual de factory functions.

Decisiones técnicas relevantes
KMP con Compose Multiplatform se eligió sobre React Native por la complejidad del proceso de build de RN, especialmente en iOS, y por aprovechar el background existente en Android nativo con Kotlin y Jetpack Compose.
multiplatform-settings se eligió sobre SQLDelight porque la estructura de datos del progreso es simple y no justifica el overhead de una base de datos relacional en esta etapa.
ValidatePronunciationUseCase vive en commonMain como código puro sin dependencias de plataforma, completamente testeable en commonTest sin emulador.
El código específico de plataforma se limita exclusivamente a los tres actual de voz y conectividad más los entry points mínimos de cada plataforma.

MVP — primera versión
Incluye perfil local automático, 15 niveles con una palabra por nivel, tres juegos por nivel, TTS en ambas plataformas, STT con fallback por amplitud en el juego 3 y navegación completa. Queda fuera del MVP cualquier backend, login, animaciones avanzadas o integración con servicios externos.




























Perfecto, acá van todos los prompts en español:

Prompts para Claude Code — Aplivit KMP

Tarea 1 — Gradle setup
Configurá los archivos gradle del proyecto KMP Aplivit.

Actualizá gradle/libs.versions.toml con estas versiones exactas:
- kotlin 2.1.20
- compose-multiplatform 1.8.0
- koin 3.5.6, koin-compose 1.1.5
- lifecycle 2.9.0-alpha03
- navigation-compose 2.8.0-alpha10
- kotlinx-serialization-json 1.7.3
- multiplatform-settings-no-arg 1.1.1
- kotlinx-coroutines-core 1.9.0
- agp 8.5.2

Actualizá composeApp/build.gradle.kts declarando targets: androidTarget + iosX64 + iosArm64 + iosSimulatorArm64.

Dependencias commonMain: compose runtime/foundation/material3/ui/resources, koin-core, koin-compose, lifecycle-viewmodel, navigation-compose, serialization-json, multiplatform-settings-no-arg, coroutines-core.

Dependencias androidMain: koin-android, lifecycle-viewmodel-compose.

Config Android: namespace com.aplivit, minSdk 24, compileSdk 35, jvmTarget 11.

No agregues dependencias que no estén en esta lista.


Tarea 2 — Entidades y puertos
En composeApp/src/commonMain/kotlin/com/aplivit/ creá la capa de dominio:

MODELOS en core/domain/model/:
- Level.kt — data class: id: Int, syllables: List<Syllable>, word: String, instruction: String. Anotada con @Serializable.
- Syllable.kt — data class: text: String. Anotada con @Serializable.
- UserProgress.kt — data class: currentLevel: Int = 1, completedLevels: Set<Int> = emptySet(), totalErrors: Int = 0.
- GameResult.kt — sealed class con object Success y data class Failure(val attempts: Int).

PUERTOS en core/port/:
- ProgressRepository.kt — interface con loadProgress(): UserProgress y saveProgress(progress: UserProgress).
- SpeechSynthesizer.kt — interface con speak(text: String), stop(), release().
- SpeechRecognizer.kt — interface con val mode: RecognitionMode, startListening(expected: String, onResult: (RecognitionResult) -> Unit), stopListening(). En el mismo archivo declarar enum RecognitionMode { STT, AMPLITUDE } y sealed class RecognitionResult con Transcription(text: String), SoundDetected, NoSound, Error.
- ConnectivityChecker.kt — interface con isConnected(): Boolean.

Solo interfaces y modelos, sin implementaciones todavía.


Tarea 3 — Casos de uso
En composeApp/src/commonMain/kotlin/com/aplivit/core/domain/usecase/ creá los casos de uso:

GetLevelsUseCase.kt — recibe LevelsLoader como dependencia, expone suspend fun execute(): List<Level> que delega en levelsLoader.load().

CompleteGameUseCase.kt — recibe ProgressRepository. Función execute(levelId: Int, errors: Int): carga el progreso actual, agrega levelId a completedLevels, suma errors a totalErrors, guarda.

UnlockNextLevelUseCase.kt — recibe ProgressRepository. Función execute(completedLevelId: Int): carga progreso, si currentLevel == completedLevelId entonces currentLevel++, guarda.

ValidatePronunciationUseCase.kt — sin dependencias. Función execute(result: RecognitionResult, expected: String): Boolean.
  - Si result es Transcription: normalizar ambos strings a mayúsculas sin tildes, retornar true si el texto normalizado contiene la palabra esperada normalizada.
  - Si result es SoundDetected: retornar true.
  - Si result es NoSound o Error: retornar false.

No uses coroutines en ValidatePronunciationUseCase, es lógica pura sincrónica.


Tarea 4 — levels.json + LevelsLoader
Creá el archivo composeApp/src/commonMain/composeResources/files/levels.json con estos 15 niveles:

[
  {"id":1,"syllables":["MA","MA"],"word":"MAMA","instruction":"En este nivel aprenderemos MA. Escuchá y repetí."},
  {"id":2,"syllables":["ME","SA"],"word":"MESA","instruction":"Ahora practicamos ME y SA."},
  {"id":3,"syllables":["MI","SA"],"word":"MISA","instruction":"Combinamos MI y SA."},
  {"id":4,"syllables":["PA","PA"],"word":"PAPA","instruction":"Aprendemos PA."},
  {"id":5,"syllables":["PE","CA"],"word":"PECA","instruction":"Practicamos PE y CA."},
  {"id":6,"syllables":["PI","PA"],"word":"PIPA","instruction":"Combinamos PI y PA."},
  {"id":7,"syllables":["SA","LA"],"word":"SALA","instruction":"Aprendemos SA y LA."},
  {"id":8,"syllables":["SE","TA"],"word":"SETA","instruction":"Practicamos SE y TA."},
  {"id":9,"syllables":["SI","MA"],"word":"SIMA","instruction":"Combinamos SI y MA."},
  {"id":10,"syllables":["LA","CA"],"word":"LACA","instruction":"Aprendemos LA y CA."},
  {"id":11,"syllables":["LE","MA"],"word":"LEMA","instruction":"Practicamos LE y MA."},
  {"id":12,"syllables":["LI","MA"],"word":"LIMA","instruction":"Combinamos LI y MA."},
  {"id":13,"syllables":["CA","MA"],"word":"CAMA","instruction":"Aprendemos CA y MA."},
  {"id":14,"syllables":["TA","PA"],"word":"TAPA","instruction":"Practicamos TA y PA."},
  {"id":15,"syllables":["TE","LA"],"word":"TELA","instruction":"Combinamos TE y LA."}
]

Luego creá infrastructure/content/LevelsLoader.kt en commonMain:
- Clase LevelsLoader sin dependencias.
- Función suspend fun load(): List<Level> que lee el archivo con Res.readBytes("files/levels.json"), lo convierte a String y lo deserializa con Json { ignoreUnknownKeys = true }.
- Importar correctamente org.jetbrains.compose.resources.Res y androidx.compose.resources.


Tarea 5 — Persistencia
Creá infrastructure/storage/SettingsProgressRepository.kt en commonMain:

- Implementa ProgressRepository.
- Recibe Settings como dependencia en el constructor.
- loadProgress(): lee tres claves:
    "current_level" como Int con default 1
    "completed_levels" como String con default ""
    "total_errors" como Int con default 0
  Convierte completed_levels a Set<Int> haciendo split(",") y filtrando blancos.
  Retorna UserProgress con esos valores.
- saveProgress(progress): escribe las tres claves. completedLevels se serializa con joinToString(",").

Creá infrastructure/PlatformProviders.kt en commonMain con estas funciones expect:
    expect fun provideSpeechSynthesizer(): SpeechSynthesizer
    expect fun provideSpeechRecognizer(connectivityChecker: ConnectivityChecker): SpeechRecognizer
    expect fun provideConnectivityChecker(): ConnectivityChecker
    expect fun provideSettings(): Settings

No crees los actual todavía.


Tarea 6 — DI con Koin
Creá di/AppModule.kt en commonMain:

val appModule = module {
    single<ConnectivityChecker> { provideConnectivityChecker() }
    single<SpeechSynthesizer>   { provideSpeechSynthesizer() }
    single<SpeechRecognizer>    { provideSpeechRecognizer(get()) }
    single                      { provideSettings() }
    single<ProgressRepository>  { SettingsProgressRepository(get()) }
    single                      { LevelsLoader() }
    factory { GetLevelsUseCase(get()) }
    factory { CompleteGameUseCase(get()) }
    factory { ValidatePronunciationUseCase() }
    factory { UnlockNextLevelUseCase(get()) }
}

Los ViewModels NO van en este módulo.


Tarea 7 — actual Android (TTS, STT, Connectivity, Settings)
En composeApp/src/androidMain/kotlin/com/aplivit/infrastructure/ creá los actual de PlatformProviders:

Primero creá un objeto AppContext en androidMain:
    object AppContext {
        lateinit var context: Context
    }

provideSettings(): retorna SharedPreferencesSettings(PreferenceManager.getDefaultSharedPreferences(AppContext.context)) con nombre de archivo "aplivit_prefs".

provideConnectivityChecker(): retorna una implementación anónima de ConnectivityChecker que usa ConnectivityManager + NetworkCapabilities.NET_CAPABILITY_INTERNET.

provideSpeechSynthesizer(): retorna AndroidSpeechSynthesizer(AppContext.context). Creá la clase AndroidSpeechSynthesizer que implementa SpeechSynthesizer usando android.speech.tts.TextToSpeech con idioma Locale("es", "ES").

provideSpeechRecognizer(connectivityChecker): si connectivityChecker.isConnected() retorna AndroidSpeechRecognizer(AppContext.context), si no retorna AmplitudeSpeechRecognizer(AppContext.context).
- AndroidSpeechRecognizer usa android.speech.SpeechRecognizer con RecognizerIntent, idioma es-ES, llama onResult con Transcription.
- AmplitudeSpeechRecognizer usa AudioRecord, mide amplitud cada 100ms durante 3 segundos, umbral 5000, llama onResult con SoundDetected si supera el umbral o NoSound si no.

Actualizá MainActivity para inicializar AppContext.context = this antes de startKoin.


Tarea 8 — actual iOS (stubs)
En composeApp/src/iosMain/kotlin/com/aplivit/infrastructure/ creá los actual de PlatformProviders para iOS:

provideSettings(): retorna NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults).

provideConnectivityChecker(): stub que siempre retorna true.

provideSpeechSynthesizer(): retorna IosSpeechSynthesizer que implementa SpeechSynthesizer usando AVSpeechSynthesizer + AVSpeechUtterance con voz es-ES y rate 0.5f.

provideSpeechRecognizer(connectivityChecker): retorna IosSpeechRecognizer. En modo STT usa SFSpeechRecognizer + SFSpeechAudioBufferRecognitionRequest + AVAudioEngine. En modo AMPLITUDE retorna SoundDetected directamente como stub.

No rompas la compilación de commonMain, estos actual solo se usan al compilar iOS.


Tarea 9 — Navegación y App.kt
Creá presentation/navigation/AppNavigation.kt en commonMain:
- Tres rutas: "home", "level/{levelId}", "game/{levelId}".
- NavHost con startDestination "home".
- HomeScreen en ruta "home", recibe navController.
- LevelScreen en ruta "level/{levelId}", extrae levelId como Int del backStackEntry.
- GameScreen en ruta "game/{levelId}", extrae levelId como Int, recibe onCompleted: () -> Unit que navega popBackStack hasta "home".

Actualizá App.kt en commonMain para que llame a AppNavigation() dentro de MaterialTheme.


Tarea 10 — ViewModels y pantallas
Creá los ViewModels y pantallas en commonMain:

HomeViewModel: recibe GetLevelsUseCase y ProgressRepository via constructor. Estado HomeUiState(levels: List<Level>, progress: UserProgress, isLoading: Boolean). En init lanza coroutine para cargar niveles y progreso. Expone también speakWelcome() que llama SpeechSynthesizer con "Bienvenido a Aplivit. Elegí un nivel para comenzar.".

LevelViewModel(levelId: Int): recibe GetLevelsUseCase y SpeechSynthesizer. Estado LevelUiState(level: Level?, isLoading: Boolean). En init carga el nivel y reproduce level.instruction. Expone speakSyllable(text: String).

GameViewModel(levelId: Int): recibe GetLevelsUseCase, CompleteGameUseCase, UnlockNextLevelUseCase, ValidatePronunciationUseCase, SpeechRecognizer, SpeechSynthesizer. Estado GameUiState con level, currentStep (enum: DRAG_DROP, SELECTION, REPEAT, COMPLETED), feedback: String?, errors: Int, isListening: Boolean. Implementá onDragDropCompleted(correct), onSelectionCompleted(correct), startListening(expected), stopListening(). Al completar REPEAT exitosamente llamar CompleteGameUseCase y UnlockNextLevelUseCase y pasar a COMPLETED.

Creá HomeScreen, LevelScreen, GameScreen (contenedor), DragDropGameScreen, SelectionGameScreen, RepeatGameScreen usando los componentes SyllableCard, LevelCard y AudioButton según el contexto del proyecto. Cada pantalla recibe su ViewModel via koinInject() + viewModel {}.


Con estos 10 prompts cubrís todo el proyecto de punta a punta. Ejecutalos en orden, uno por uno, y verificá que compila antes de pasar al siguiente. ¿Arrancamos?

