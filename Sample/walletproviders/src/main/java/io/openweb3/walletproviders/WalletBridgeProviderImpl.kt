package io.openweb3.walletproviders

import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.LifecycleOwner
import io.openweb3.plugins.bridge.BridgeProvider
import java.util.LinkedList

class WalletBridgeProviderImpl(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val dappUrl: String
    ) : BridgeProvider{

    companion object {
        private const val CHAIN_ID = 56
        private const val RPC_URL = "https://bsc-dataseed2.binance.org"
    }
    override fun getWebClient() = webViewClient

    private var webView: WebView? = null

    private val jsExecuteQueue = LinkedList<String>()

    private var isPageLoaded = false
        set(value) {
            field = value
            if (value) {
                executeJsQueue()
            }
        }

    private val webViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            webView = view
            isPageLoaded = true
            initBridge()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return  false
        }
    }

    override fun onWebViewCreated(webView: WebView) {
        WebAppInterface(context = context, webView = webView, dappUrl = dappUrl).apply {
            webView.addJavascriptInterface(this, "_tw_")
        }
    }

    override fun onWebViewDestroy(webView: WebView) {
    }

    private fun initBridge() {
        val provderJs = loadProviderJs()
        val initJs = loadInitJs(
            CHAIN_ID,
            RPC_URL
        )
        executeJS(provderJs)
        executeJS(initJs)

//        val rpcUrl = mChain.hosts?.find {
//            !it.isNullOrEmpty()
//        } ?: "https://rpc.alg2.algen.network/"
//        val address = viewModel.getAddress(mChain.chainCode.toString()).orEmpty()
//        val chainId = mChain.chainId
//        val tronRpcUrl = viewModel.findChain(ChainCode.TRON)?.hosts?.firstOrNull()
//            ?: ConfigurationManager.getInstance().getDefaultTronRpcUrl()
//        val tronAddress = viewModel.getAddress(ChainCode.TRON.code).orEmpty()
//
//        val providerJs = DAppWebInitializer.loadProviderJs(requireContext())
//        val initJs = DAppWebInitializer.loadInitJs(
//            chainId = chainId,
//            rpcUrl = rpcUrl,
//            address = address,
//            tronRpcUrl = tronRpcUrl,
//            tronAddress = tronAddress,
//            tribeId = tribeId.orEmpty()
//        )
//
//        webView?.evaluateJavascript(providerJs, null)
//        webView?.evaluateJavascript(initJs, null)
//
//        val script = "document.getElementsByName(\"full-screen\")[0].content"
//        webView?.evaluateJavascript(script) { value ->
//            if (value == null) {
//                return@evaluateJavascript
//            }
//            if (value.equals("1") || value.equals("yes") || value.equals("true")
//                || value.equals("\"yes\"") || value.equals("\"true\"") || value.equals("\"1\"")
//            ) {
//                binding.groupTitle.isVisible = false
//                val dAppUrl = dApp?.findHttpUrl()
//                WebH5Preference.put(key = dAppUrl + "_fullScreen", value = 1, async = true)
//            }
//        }
    }

    private fun loadProviderJs(): String {
        return context.resources.openRawResource(R.raw.trust_min).bufferedReader().use { it.readText() }
    }

    private fun loadInitJs(chainId: Int, rpcUrl: String): String {
        val source = """
        (function() {
            var config = {                
                ethereum: {
                    chainId: $chainId,
                    rpcUrl: "$rpcUrl"
                },
                solana: {
                    cluster: "mainnet-beta",
                },
                isDebug: true
            };
            trustwallet.ethereum = new trustwallet.Provider(config);
            trustwallet.solana = new trustwallet.SolanaProvider(config);
            trustwallet.postMessage = (json) => {
                window._tw_.postMessage(JSON.stringify(json));
            }
            window.ethereum = trustwallet.ethereum;
        })();
        """
        return  source
    }

    private fun executeJS(code: String) {
        if (isPageLoaded) {
            evaluateJavascript(code)
        } else {
            jsExecuteQueue.add(code)
        }
    }

    private fun evaluateJavascript(code: String) {
        webView?.evaluateJavascript(code, null)
    }

    private fun executeJsQueue() {
        while (jsExecuteQueue.isNotEmpty()) {
            jsExecuteQueue.poll()?.let { evaluateJavascript(it) }
        }
    }
}