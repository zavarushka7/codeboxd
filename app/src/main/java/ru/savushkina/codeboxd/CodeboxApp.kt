package ru.savushkina.codeboxd

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Точка входа всего приложения
 * Application - класс, который живет все время существования процесса приложения
 * Он создается раньше любой Activity. Android берет его из манифеста (android:name=".CodeboxApp")
 *
 * @HiltAndroidApp говорит Hilt сгенерировтаь базовый компонент-контейнер зависимостей, привязанный к жц приложения
 */
@HiltAndroidApp
class CodeboxApp : Application()