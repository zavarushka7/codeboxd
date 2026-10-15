package ru.savushkina.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.savushkina.core.designsystem.theme.CodeboxTheme
import ru.savushkina.core.designsystem.util.LocalTextCaseStyle
import ru.savushkina.core.designsystem.util.toDisplayCase

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    val style = LocalTextCaseStyle.current
    LabelText(
        text = "// $text".toDisplayCase(style),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SectionHeaderPreview() {
    CodeboxTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(text = "codebox")
            SectionHeader(text = "user_profile")
            SectionHeader(text = "select_role")
        }
    }
}