package com.claudeusage.app.web

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.claudeusage.app.Graph

/**
 * Hosts a WebView where the user logs into claude.ai with their own account.
 * Two things happen here:
 *  1. We continuously snapshot the session cookies once a session exists.
 *  2. An injected hook forwards the page's own fetch/XHR responses to
 *     [UsageCaptureBridge], which persists usage as soon as the site loads it.
 * The user taps "Done" once signed in (the button highlights automatically when
 * a session is detected).
 */
class WebLoginActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var doneButton: Button
    private var sessionDetected = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF14110D.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(12), dp(12))
        }
        val title = TextView(this).apply {
            text = "Sign in to claude.ai"
            setTextColor(0xFFEFE9DD.toInt())
            textSize = 17f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        doneButton = Button(this).apply {
            text = "Done"
            isEnabled = false
            setOnClickListener { finishWithSession() }
        }
        header.addView(title)
        header.addView(doneButton)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        }

        root.addView(header)
        root.addView(webView)
        setContentView(root)

        configureWebView()
        webView.loadUrl("https://claude.ai/login")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = userAgentString.replace("; wv", "") // present as a normal browser
        }
        webView.addJavascriptInterface(
            UsageCaptureBridge(this) { runOnUiThread { onUsageCaptured() } },
            "UsageBridge",
        )
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.evaluateJavascript(HOOK_JS, null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.evaluateJavascript(HOOK_JS, null)
                refreshSessionState()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
        }
    }

    private fun refreshSessionState() {
        val cookies = CookieManager.getInstance().getCookie("https://claude.ai") ?: ""
        val hasSession = cookies.contains("sessionKey", ignoreCase = true) ||
            cookies.contains("__Secure", ignoreCase = true)
        if (hasSession && !sessionDetected) {
            sessionDetected = true
            doneButton.isEnabled = true
            doneButton.text = "Done ✓"
            Toast.makeText(this, "Signed in — loading your usage…", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onUsageCaptured() {
        // Usage already persisted by the bridge; if the user is signed in we can
        // finish automatically for a smooth flow.
        if (sessionDetected) finishWithSession()
    }

    private fun finishWithSession() {
        val cookies = CookieManager.getInstance().getCookie("https://claude.ai")
        if (cookies.isNullOrBlank()) {
            Toast.makeText(this, "Please finish signing in first.", Toast.LENGTH_SHORT).show()
            return
        }
        CookieManager.getInstance().flush()
        Graph.webSession(this).saveSession(cookies, "claude.ai account")
        setResult(RESULT_OK)
        finish()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        // Monkeypatches fetch + XHR so every response body is forwarded to the
        // native bridge. Idempotent; injected on page start and finish.
        const val HOOK_JS = """
(function(){
  if (window.__cuHook) return; window.__cuHook = true;
  function looks(s){ return typeof s==='string' && /resets?_at|reset_at|utilization|five_hour|seven_day|weekly|sonnet|rate_limit|remaining|used/i.test(s); }
  function report(u,b,h){ try{ if(window.UsageBridge && (looks(b)||looks(u))) UsageBridge.capture(String(u||''), String(b||''), String(h||'')); }catch(e){} }
  try {
    var of = window.fetch;
    if (of) window.fetch = function(){
      var args = arguments;
      return of.apply(this,args).then(function(res){
        try { var u = (res&&res.url) || (args[0]&&args[0].url) || args[0];
          res.clone().text().then(function(t){ report(u,t,''); }).catch(function(){}); } catch(e){}
        return res;
      });
    };
  } catch(e){}
  try {
    var X = window.XMLHttpRequest;
    if (X) {
      var open = X.prototype.open, send = X.prototype.send;
      X.prototype.open = function(m,u){ this.__u=u; return open.apply(this,arguments); };
      X.prototype.send = function(){
        var self=this;
        this.addEventListener('load', function(){ try{ report(self.__u, self.responseText, self.getAllResponseHeaders&&self.getAllResponseHeaders()); }catch(e){} });
        return send.apply(this,arguments);
      };
    }
  } catch(e){}
})();
"""
    }
}
