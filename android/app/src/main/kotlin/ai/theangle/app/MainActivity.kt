package ai.theangle.app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var splashView: View

    private val appUrl = "https://theangle.ai/app.html"
    private val allowedHosts = setOf("theangle.ai", "www.theangle.ai")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.parseColor("#0a0a1a")
        window.navigationBarColor = Color.parseColor("#0a0a1a")

        setContentView(R.layout.activity_main)

        splashView = findViewById(R.id.splashView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)

        setupWebView()
        setupSwipeRefresh()

        webView.loadUrl(appUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportMultipleWindows(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = false
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
        }

        webView.addJavascriptInterface(AngleBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                hideSplash()
                swipeRefresh.isRefreshing = false
                view?.evaluateJavascript("""
                    (function() {
                        window.__THEANGLE_PLATFORM = 'android';
                        window.__THEANGLE_ANDROID = true;
                    })();
                """.trimIndent(), null)
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    swipeRefresh.isRefreshing = false
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: ""
                val scheme = url.scheme ?: ""

                if (host in allowedHosts) return false

                if (scheme == "http" || scheme == "https") {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    return true
                }

                try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) {}
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 100) swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(Color.parseColor("#8B5CF6"))
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#141428"))
        swipeRefresh.setOnRefreshListener { webView.reload() }
    }

    private fun hideSplash() {
        if (splashView.visibility == View.VISIBLE) {
            splashView.animate().alpha(0f).setDuration(400)
                .withEndAction { splashView.visibility = View.GONE }.start()
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    inner class AngleBridge {
        @JavascriptInterface
        fun getPlatform(): String = "android"
    }
}
