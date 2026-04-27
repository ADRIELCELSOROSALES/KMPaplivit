package com.aplivit.presentation.screen.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import com.aplivit.core.domain.model.AppLanguage
import com.aplivit.core.port.ProgressRepository
import com.aplivit.core.port.SpeechSynthesizer
import com.aplivit.shared.stringsFor
import org.koin.compose.koinInject

private val languageFlags = mapOf(
    AppLanguage.SPANISH to "🇦🇷",
    AppLanguage.ENGLISH to "🇺🇸",
    AppLanguage.FRENCH to "🇫🇷"
)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo: ProgressRepository = koinInject()
    val tts: SpeechSynthesizer = koinInject()
    val vm: SettingsViewModel = remember { SettingsViewModel(repo, tts) }
    val state by vm.state.collectAsState()

    val strings = stringsFor(state.selectedLanguage)

    LaunchedEffect(Unit) {
        tts.speakAndWait(strings.settings)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = strings.settings,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1565C0),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = strings.selectLanguage,
            fontSize = 16.sp,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        AppLanguage.entries.forEach { language ->
            val isSelected = language == state.selectedLanguage
            LanguageCard(
                language = language,
                flag = languageFlags[language] ?: "",
                isSelected = isSelected,
                onClick = {
                    vm.selectLanguage(language)
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    flag: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF4CAF50) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flag, fontSize = 32.sp)
            Text(
                text = "  ${language.displayName}",
                fontSize = 22.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color(0xFF1565C0)
            )
        }
    }
}
