package com.hoshi.reader.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
class ReaderWebView(
    context: Context,
    private val contentDir: File,
    private val callbacks: Callbacks
) : WebView(context) {

    interface Callbacks {
        fun onRestoreCompleted()
        fun onTextSelected(json: String)
        fun onSelectionState(hasSelection: Boolean)
        fun onPageChanged(direction: String)
    }

    private val gestureDetector: GestureDetector
    private var pendingStyles: (() -> Unit)? = null
    private var pageLoaded = false

    init {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.useWideViewPort = false
        settings.loadWithOverviewMode = false

        setBackgroundColor(0xFFFFFFFF.toInt())

        addJavascriptInterface(HoshiBridge(), "HoshiBridge")

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d("HoshiWebView", "${msg.sourceId()}:${msg.lineNumber()} ${msg.message()}")
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url
                if (url.scheme == "file") {
                    val path = url.path ?: return null
                    val file = File(path)
                    if (file.exists()) {
                        val mimeType = guessMimeType(path)
                        return WebResourceResponse(mimeType, "UTF-8", file.inputStream())
                    }
                    Log.w("HoshiWebView", "File not found: $path")
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                Log.d("HoshiWebView", "onPageFinished: $url")
                pageLoaded = true
                injectScripts()
                pendingStyles?.invoke()
                pendingStyles = null
            }
        }

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val width = this@ReaderWebView.width.toFloat()
                val tapX = e.x
                when {
                    tapX < width * 0.3f -> callbacks.onPageChanged("backward")
                    tapX > width * 0.7f -> callbacks.onPageChanged("forward")
                }
                return true
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    fun loadChapter(href: String) {
        pageLoaded = false
        val chapterFile = File(contentDir, href)
        Log.d("HoshiWebView", "loadChapter: href=$href, file=${chapterFile.absolutePath}, exists=${chapterFile.exists()}")
        if (chapterFile.exists()) {
            loadUrl("file://${chapterFile.absolutePath}")
        } else {
            Log.e("HoshiWebView", "Chapter file not found: ${chapterFile.absolutePath}")
        }
    }

    private fun injectScripts() {
        val scripts = listOf("reader.js", "selection.js", "highlights.js")
        for (script in scripts) {
            try {
                val js = context.assets.open("reader/$script").bufferedReader().readText()
                evaluateJavascript(js, null)
            } catch (e: Exception) {
                Log.e("HoshiWebView", "Failed to inject $script", e)
            }
        }
        Log.d("HoshiWebView", "Scripts injected")
    }

    fun applyStyles(fontSize: Int, lineHeight: Double, verticalWriting: Boolean, theme: String, customCss: String) {
        val action = {
            val bgColor: String
            val textColor: String
            when (theme) {
                "DARK" -> { bgColor = "#1A1A1A"; textColor = "#E0E0E0" }
                "SEPIA" -> { bgColor = "#F5E6C8"; textColor = "#5B4636" }
                else -> { bgColor = "#FFFFFF"; textColor = "#1A1A1A" }
            }

            val writingMode = if (verticalWriting) "vertical-rl" else "horizontal-tb"
            val columnCss = if (verticalWriting) {
                "height: 100vh; overflow-y: hidden; overflow-x: auto; scroll-snap-type: x mandatory;"
            } else {
                "height: auto; column-width: 100vw; column-gap: 0; overflow-x: hidden; overflow-y: auto; scroll-snap-type: y mandatory;"
            }

            val css = """
                html { height: 100%; margin: 0; padding: 0; }
                body {
                    font-size: ${fontSize}px;
                    line-height: $lineHeight;
                    writing-mode: $writingMode;
                    background-color: $bgColor;
                    color: $textColor;
                    margin: 0;
                    padding: 16px;
                    box-sizing: border-box;
                    $columnCss
                    -webkit-text-size-adjust: none;
                    word-break: break-all;
                }
                ::highlight(hoshi-selection) {
                    background-color: rgba(100, 149, 237, 0.4);
                }
                .hoshi-sasayaki-active {
                    background-color: var(--hoshi-sasayaki-background-color, rgba(255, 200, 50, 0.3));
                }
                .hoshi-highlight-yellow { background-color: rgba(255, 255, 0, 0.35); }
                .hoshi-highlight-green { background-color: rgba(0, 255, 0, 0.25); }
                .hoshi-highlight-blue { background-color: rgba(100, 149, 237, 0.35); }
                .hoshi-highlight-pink { background-color: rgba(255, 105, 180, 0.35); }
                .hoshi-highlight-orange { background-color: rgba(255, 165, 0, 0.35); }
                img { max-width: 100%; height: auto; }
                $customCss
            """.trimIndent()

            val js = """
                (function() {
                    var style = document.getElementById('hoshi-styles');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'hoshi-styles';
                        document.head.appendChild(style);
                    }
                    style.textContent = ${escapeJs(css)};
                    window.hoshiReader.pageHeight = window.innerHeight;
                    window.hoshiReader.pageWidth = window.innerWidth;
                    window.hoshiReader.registerCopyText();
                    window.hoshiReader.buildNodeOffsets();
                    console.log('Styles applied. pageHeight=' + window.innerHeight + ' pageWidth=' + window.innerWidth + ' writingMode=' + getComputedStyle(document.body).writingMode + ' scrollH=' + document.body.scrollHeight + ' scrollW=' + document.body.scrollWidth + ' bodyH=' + document.body.offsetHeight + ' bodyW=' + document.body.offsetWidth + ' children=' + document.body.childElementCount + ' htmlLen=' + document.body.innerHTML.length);
                })();
            """.trimIndent()
            evaluateJavascript(js, null)
        }

        if (pageLoaded) {
            action()
        } else {
            pendingStyles = action
        }
    }

    fun restoreProgress(progress: Double) {
        if (!pageLoaded) return
        Log.d("HoshiWebView", "restoreProgress: $progress")
        evaluateJavascript("window.hoshiReader.restoreProgress($progress);", null)
    }

    fun paginate(direction: String) {
        evaluateJavascript("window.hoshiReader.paginate('$direction');") { result ->
            Log.d("HoshiWebView", "paginate($direction) = $result")
            if (result?.contains("limit") == true) {
                callbacks.onPageChanged(if (direction == "forward") "next_chapter" else "prev_chapter")
            }
        }
    }

    fun getProgress(callback: (Double) -> Unit) {
        evaluateJavascript("window.hoshiReader.calculateProgress();") { result ->
            callback(result?.toDoubleOrNull() ?: 0.0)
        }
    }

    fun highlightSasayakiCue(textStart: Int, textEnd: Int) {
        val js = """
            (function() {
                document.querySelectorAll('.hoshi-sasayaki-active').forEach(el => el.classList.remove('hoshi-sasayaki-active'));
                if ($textStart < 0 || $textEnd <= $textStart) return;
                var offsets = window.hoshiReader?.nodeOffsets;
                if (!offsets) return;
                for (var i = 0; i < offsets.length; i++) {
                    var node = offsets[i];
                    if (node.end > $textStart && node.start < $textEnd) {
                        var range = document.createRange();
                        var startOff = Math.max(0, $textStart - node.start);
                        var endOff = Math.min(node.text.length, $textEnd - node.start);
                        range.setStart(node.node, startOff);
                        range.setEnd(node.node, endOff);
                        var span = document.createElement('span');
                        span.className = 'hoshi-sasayaki-active';
                        range.surroundContents(span);
                    }
                }
            })();
        """.trimIndent()
        evaluateJavascript(js, null)
    }

    fun clearSasayakiHighlight() {
        evaluateJavascript(
            "document.querySelectorAll('.hoshi-sasayaki-active').forEach(el => { el.outerHTML = el.innerHTML; });",
            null
        )
    }

    private fun guessMimeType(path: String): String = when {
        path.endsWith(".css") -> "text/css"
        path.endsWith(".js") -> "application/javascript"
        path.endsWith(".png") -> "image/png"
        path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
        path.endsWith(".gif") -> "image/gif"
        path.endsWith(".svg") -> "image/svg+xml"
        path.endsWith(".xhtml") || path.endsWith(".html") || path.endsWith(".htm") -> "application/xhtml+xml"
        path.endsWith(".woff") -> "font/woff"
        path.endsWith(".woff2") -> "font/woff2"
        path.endsWith(".ttf") || path.endsWith(".otf") -> "font/opentype"
        else -> "application/octet-stream"
    }

    private fun escapeJs(s: String): String {
        val escaped = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }

    inner class HoshiBridge {
        @JavascriptInterface
        fun restoreCompleted() {
            Log.d("HoshiWebView", "restoreCompleted callback")
            post { callbacks.onRestoreCompleted() }
        }

        @JavascriptInterface
        fun textSelected(json: String) {
            post { callbacks.onTextSelected(json) }
        }

        @JavascriptInterface
        fun selectionState(hasSelection: Boolean) {
            post { callbacks.onSelectionState(hasSelection) }
        }
    }
}
