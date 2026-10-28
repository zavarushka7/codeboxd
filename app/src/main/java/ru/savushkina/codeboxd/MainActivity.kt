package ru.savushkina.codeboxd

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.feature.settings.SettingsViewModel

/**
 * @AndroidEntryPoint - парная аннотация к @HiltAndroidApp.
 * Она говорит Hilt разрешить этой Activity получать зависимости через @Inject и использовать hiltViewModel()
 * @AndroidEntryPoint должен стоять на каждой Activity/Fragment, где я хочу использовать Hilt!
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * enableEdgeToEdge - это современный способ работы с системными панелями (status bar сверху, navigation bar снизу)
         * Он делает панели прозрачными (Color.TRANSPARENT)
         * контент рисуется под ними (edge-to-edge)
         * SystemBarStyle.dark(...) означает иконки на панелях будут светлыми (потому что фон темный)
         */
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            /**
             * hiltViewModel() - Compose-функция, которая:
             * 1. Находит или создает ViewModel через Hilt
             * 2. Привязывает ее к ближайшему ViewModelStoreOwner (обычно Activity или NavBackStackEntry)
             *
             * SettingsViewModel берется на уровне Activity - значит он будет жить, пока жива Activity.
             * Что правильно, тк настройки темы нужны всему приложению
             */
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            /**
             * collectAsStateWithLifecycle() - превращает Flow/StateFlow в Compose State, но с учетом жц.
             * Когда Activity уходит в фон - подписка приостанавливается, экономя ресурсы
             * by - это делегирование свойства: val textCaseStyle by ... означает читай значение из State.value автоматически
             * Внутри Compose каждый раз при изменении State произойдет рекомпозиция
             */
            val textCaseStyle by settingsViewModel.textCaseStyle.collectAsStateWithLifecycle()

            /**
             * CpdeboxTheme(textCaseStyle = textCaseStyle) { ... } - оборачивает все в тему
             */
            CodeboxTheme(textCaseStyle = textCaseStyle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    /**
                     * rememberNavController() создает NavHostController один раз за жизнь композиции.
                     * Если обернуть remember, контроллер выживет при рекомпозиции
                     */
                    CodeboxNavHost(navController = rememberNavController())
                }
            }
        }
    }
}