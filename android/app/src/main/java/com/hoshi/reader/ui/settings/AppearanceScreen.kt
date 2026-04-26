package com.hoshi.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hoshi.reader.core.storage.ReaderTheme
import com.hoshi.reader.core.storage.UserConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    userConfig: UserConfig,
    onBack: () -> Unit
) {
    val fontSize by userConfig.fontSize.collectAsState(initial = 18)
    val lineHeight by userConfig.lineHeight.collectAsState(initial = 1.8)
    val verticalWriting by userConfig.verticalWriting.collectAsState(initial = true)
    val theme by userConfig.theme.collectAsState(initial = ReaderTheme.LIGHT)
    val customCss by userConfig.customCss.collectAsState(initial = "")
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Font Size: $fontSize")
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { scope.launch { userConfig.setFontSize(it.toInt()) } },
                valueRange = 12f..32f,
                steps = 19,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            SectionTitle("Line Height: ${"%.1f".format(lineHeight)}")
            Slider(
                value = lineHeight.toFloat(),
                onValueChange = { scope.launch { userConfig.setLineHeight(it.toDouble()) } },
                valueRange = 1.0f..3.0f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Vertical Writing", modifier = Modifier.weight(1f))
                Switch(
                    checked = verticalWriting,
                    onCheckedChange = { scope.launch { userConfig.setVerticalWriting(it) } }
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionTitle("Theme")
            val themes = ReaderTheme.entries.toList()
            val selectedIndex = themes.indexOf(theme)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themes.forEachIndexed { index, t ->
                    SegmentedButton(
                        selected = index == selectedIndex,
                        onClick = { scope.launch { userConfig.setTheme(t) } },
                        shape = SegmentedButtonDefaults.itemShape(index, themes.size)
                    ) {
                        Text(t.name)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionTitle("Custom CSS")
            var cssText by remember(customCss) { mutableStateOf(customCss) }
            OutlinedTextField(
                value = cssText,
                onValueChange = {
                    cssText = it
                    scope.launch { userConfig.setCustomCss(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 10
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
