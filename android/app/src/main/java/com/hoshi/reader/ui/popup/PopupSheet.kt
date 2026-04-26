package com.hoshi.reader.ui.popup

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.hoshi.reader.core.dictionary.LookupResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupSheet(
    selectedText: String,
    onDismiss: () -> Unit,
    onMineEntry: (String) -> Unit = {},
    viewModel: PopupViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val results by viewModel.lookupResults.collectAsState()
    val stylesJson by viewModel.styles.collectAsState()
    val config = LocalConfiguration.current
    val maxHeight = (config.screenHeightDp * 0.6).dp

    LaunchedEffect(selectedText) {
        viewModel.lookup(selectedText)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        PopupWebViewContent(
            results = results,
            stylesJson = stylesJson,
            viewModel = viewModel,
            onDismiss = onDismiss,
            onMineEntry = onMineEntry,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = maxHeight)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PopupWebViewContent(
    results: Array<LookupResult>,
    stylesJson: String,
    viewModel: PopupViewModel,
    onDismiss: () -> Unit,
    onMineEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val webView = remember { mutableMapOf<String, WebView>() }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                setBackgroundColor(Color.TRANSPARENT)

                addJavascriptInterface(PopupBridge(
                    onTapOutside = { post { onDismiss() } },
                    onMineEntry = { json -> post { onMineEntry(json) } },
                    onOpenLink = { url ->
                        // TODO: open in browser
                    },
                    viewModel = viewModel
                ), "HoshiBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url
                        if (url.scheme == "image") {
                            val dictName = url.getQueryParameter("dictionary") ?: return null
                            val path = url.getQueryParameter("path") ?: return null
                            val data = viewModel.getMediaFile(dictName, path) ?: return null
                            val mimeType = when {
                                path.endsWith(".svg") -> "image/svg+xml"
                                path.endsWith(".png") -> "image/png"
                                path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
                                else -> "application/octet-stream"
                            }
                            return WebResourceResponse(mimeType, null, ByteArrayInputStream(data))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }
                }

                val html = buildPopupHtml()
                loadDataWithBaseURL("file:///android_asset/reader/", html, "text/html", "utf-8", null)
                webView["main"] = this
            }
        },
        update = { view ->
            if (results.isNotEmpty()) {
                val entriesJson = buildEntriesJson(results)
                val js = """
                    window.lookupEntries = $entriesJson;
                    window.dictionaryStyles = $stylesJson;
                    window.collapseDictionaries = false;
                    window.compactGlossaries = false;
                    window.deduplicatePitchAccents = true;
                    window.harmonicFrequency = true;
                    document.getElementById('entries-container').innerHTML = '';
                    window.renderPopup();
                """.trimIndent()
                view.evaluateJavascript(js, null)
            }
        },
        modifier = modifier
    )
}

private fun buildPopupHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <link rel="stylesheet" href="popup.css">
        </head>
        <body>
            <div id="entries-container"></div>
            <div class="overlay" onclick="closeOverlay()">
                <span class="overlay-close">✕</span>
                <div class="overlay-content"></div>
            </div>
            <script src="popup.js"></script>
        </body>
        </html>
    """.trimIndent()
}

private fun buildEntriesJson(results: Array<LookupResult>): String {
    val arr = buildJsonArray {
        for (result in results) {
            add(buildJsonObject {
                put("expression", result.term.expression)
                put("reading", result.term.reading)
                put("matched", result.matched)
                put("rules", result.term.rules)

                put("deinflectionTrace", buildJsonArray {
                    for (p in result.process) {
                        add(buildJsonObject {
                            put("name", p)
                            put("description", "")
                        })
                    }
                })

                put("glossaries", buildJsonArray {
                    for (g in result.term.glossaries) {
                        add(buildJsonObject {
                            put("dictionary", g.dictName)
                            put("content", g.glossary)
                            put("definitionTags", g.definitionTags)
                            put("termTags", g.termTags)
                        })
                    }
                })

                put("frequencies", buildJsonArray {
                    for (f in result.term.frequencies) {
                        add(buildJsonObject {
                            put("dictionary", f.dictName)
                            put("frequencies", buildJsonArray {
                                for (freq in f.frequencies) {
                                    add(buildJsonObject {
                                        put("value", freq.value)
                                        put("displayValue", freq.displayValue)
                                    })
                                }
                            })
                        })
                    }
                })

                put("pitches", buildJsonArray {
                    for (p in result.term.pitches) {
                        add(buildJsonObject {
                            put("dictionary", p.dictName)
                            put("pitchPositions", buildJsonArray {
                                for (pos in p.pitchPositions) {
                                    add(JsonPrimitive(pos))
                                }
                            })
                        })
                    }
                })
            })
        }
    }
    return arr.toString()
}

private class PopupBridge(
    private val onTapOutside: () -> Unit,
    private val onMineEntry: (String) -> Unit,
    private val onOpenLink: (String) -> Unit,
    private val viewModel: PopupViewModel
) {
    @JavascriptInterface
    fun tapOutside() {
        onTapOutside()
    }

    @JavascriptInterface
    fun mineEntry(json: String): Boolean {
        onMineEntry(json)
        return true
    }

    @JavascriptInterface
    fun duplicateCheck(expression: String): Boolean {
        return false
    }

    @JavascriptInterface
    fun openLink(url: String) {
        onOpenLink(url)
    }

    @JavascriptInterface
    fun playWordAudio(json: String) {
        // TODO: implement audio playback
    }
}
