package ru.savushkina.core.model

enum class TextCaseStyle {
    /** Обычный текст — как есть. */
    NORMAL,
    /** camelCase — первое слово с маленькой, остальные с большой. */
    CAMEL_CASE,
    /** snake_case — всё с маленькой, пробелы как подчёркивания. */
    SNAKE_CASE
}