package io.openweb3.walletproviders

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject

class WebAppInterface(
    private val context: Context,
    private val webView: WebView,
    private val dappUrl: String?
) {
    @JavascriptInterface
    fun postMessage(json: String) {
        val obj = JSONObject(json)
        println(obj)
        val id = obj.getLong("id")
        val method = DAppMethod.fromValue(obj.getString("name"))
        val network = obj.getString("network")
        when (method) {
            DAppMethod.REQUESTACCOUNTS -> {
                // TODO
            }
            DAppMethod.SIGNMESSAGE -> {
                // TODO
            }
            DAppMethod.SIGNPERSONALMESSAGE -> {
                // TODO
            }
            DAppMethod.SIGNTYPEDMESSAGE -> {
                // TODO
            }
            else -> {
            }
        }
    }

    private fun extractMessage(json: JSONObject): ByteArray {
        val param = json.getJSONObject("object")
        val data = param.getString("data")
        return Numeric.hexStringToByteArray(data)
    }

    private fun extractRaw(json: JSONObject): String {
        val param = json.getJSONObject("object")
        return param.getString("raw")
    }
}
