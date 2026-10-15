package ru.savushkina.codeboxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint

import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.domain.SettingsRepository
import ru.savushkina.core.model.TextCaseStyle
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val textCaseStyle by settingsRepository.textCaseStyle
                .collectAsStateWithLifecycle(initialValue = TextCaseStyle.NORMAL)

            CodeboxTheme(textCaseStyle = textCaseStyle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    CodeboxNavHost(navController = rememberNavController())
                }
            }
        }
    }
}