package io.openweb3.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.openweb3.plugins.PLUGIN_OPEN_PLATFORM
import io.openweb3.plugins.PluginsManager
import io.openweb3.plugins.openplatform.OpenPlatformPlugin
import io.openweb3.plugins.openplatform.miniapp.DAppLaunchWParameters
import io.openweb3.plugins.openplatform.miniapp.IMiniApp
import io.openweb3.plugins.openplatform.miniapp.WebAppLaunchWithDialogParameters
import io.openweb3.plugins.openplatform.miniapp.WebAppPreloadParameters
import io.openweb3.sample.ui.theme.MiniappandroidTheme
import io.openweb3.walletproviders.WalletBridgeProviderImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(PLUGIN_OPEN_PLATFORM)!!
private val miniAppService = openPlatformPlugin.getMiniAppService()

class MainActivity : AppCompatActivity() {
    companion object {
        // 改为应用的deeplink
        const val APP_DEEP_LINK = "sample://app"

        const val SHORTCUT_LINK = "openweb3_link"
        const val SHORTCUT_TYPE = "openweb3_type"

        const val SHORTCUT_MINIAPP = "MINIAPP"
        const val SHORTCUT_DAPP = "WEBPAGE"

    }

    private var miniApp: IMiniApp? = null

    val uriMarketPlace = Uri.Builder()
        .appendQueryParameter("roomId", "1")
        .appendQueryParameter("roomName", "Test Tribe")
        .appendQueryParameter("roomAvatar", "https://thumb.ac-illust.com/78/782445b4704adca448601a89d4b80f7c_w.jpeg")
        .appendQueryParameter("from", "space")
        .build()

    private fun preloadApps(owner: LifecycleOwner, context: Context) {
        listOf("10").forEach {
            val config = WebAppPreloadParameters.Builder()
                .owner(owner)
                .context(context)
                .startParam(uriMarketPlace.query)
                .miniAppId(it)
                .build()

            MainScope().launch {
                miniAppService.preload( config = config)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.dataString?.also {
            if (it.startsWith(APP_DEEP_LINK)) {
                val deepLink = Uri.parse(it)
                val link = deepLink.getQueryParameter(SHORTCUT_LINK)
                val type = deepLink.getQueryParameter(SHORTCUT_TYPE)
                launchWithUrlAndType(link, type)
            }
        } ?: run {
            val link = intent?.getStringExtra(SHORTCUT_LINK)
            val type = intent?.getStringExtra(SHORTCUT_TYPE)
            launchWithUrlAndType(link, type)
        }
    }

    private fun launchWithUrlAndType(link: String?, type: String?) {
        link?.also {
            if (type == SHORTCUT_MINIAPP) {
                val config = WebAppLaunchWithDialogParameters.Builder()
                    .owner(this)
                    .context(this)
                    .url(link)
                    .onDismissListener {
                    }
                    .build()

                lifecycleScope.launch {
                    miniAppService.launch(config)
                }
            } else {
                val config = DAppLaunchWParameters.Builder()
                    .owner(this)
                    .context(this)
                    .url(link)
                    .onDismissListener {
                    }
                    .build()

                lifecycleScope.launch {
                    miniAppService.launch(config)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        preloadApps(this, this)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(miniApp==null || true==miniApp?.requestDismiss()) {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val context = LocalContext.current
            val lifecycleOwner = this

            MiniappandroidTheme {
                // A surface container using the 'background' color from the theme
                ProvideWindowInsets {
                    val systemUiController = rememberSystemUiController()
                    SideEffect {
                        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            TopAppBar(
                                title = {
                                    Text(text = "MiniApp Demo")
                                },
                                contentPadding = rememberInsetsPaddingValues(
                                    insets = LocalWindowInsets.current.statusBars)
                            )
                            LaunchTgButton(lifecycleOwner) {
                                miniApp = it
                            }

                            Spacer(modifier = Modifier.height(50.dp))
                            DialogMiniAppButton(lifecycleOwner) {
                                miniApp = it
                            }
                            ChatButton(context)
                            AppMasterButton(context,lifecycleOwner)
                            MarketPlaceButton(context = context, lifecycleOwner = lifecycleOwner )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DialogMiniAppButton(lifecycleOwner: LifecycleOwner, complete: (IMiniApp?) -> Unit){

    // 对话框状态
    val showDialog = remember { mutableStateOf(false) }

    Button(modifier = Modifier.width(250.dp), onClick = {
        showDialog.value = true
    }) {
        Text(text = "Launch With Dialog")
    }

    // 对话框内容
    if (showDialog.value) {

        val context = LocalContext.current

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .botName("mini")
            .miniAppName("mini")
            .useModalStyle(true)
            .isLocal(true)
            .onDismissListener {
                showDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config).also(complete)
        }
    }
}

@Composable
fun ChatButton(context:Context) {
    Button(modifier = Modifier.width(250.dp), onClick = {
        val intent = Intent(context, ChatActivity::class.java)
        context.startActivity(intent)
    }) {
        Text(text = "Embed Launch In Chat")
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LaunchTgButton(lifecycleOwner: LifecycleOwner, complete: (IMiniApp?) -> Unit){

    // 对话框状态
    val showDialog = remember { mutableStateOf(false) }
    val showDAppDialog = remember { mutableStateOf(false) }

    var textInput by remember { mutableStateOf("https://www.magiceden.io/me") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter Telegram WebApp Launch URL Or OpenWeb3 WebApp", style = TextStyle(color = Color.Gray)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(200.dp)
        )
        Button(
            modifier = Modifier.width(250.dp),
            onClick = {
                showDialog.value = true
            }
        ) {
            Text(text = "Launch MiniApp With Url")
        }
        Button(
            modifier = Modifier.width(250.dp),
            onClick = {
                showDAppDialog.value = true
            }
        ) {
            Text(text = "Launch DApp with url")
        }
    }

    // dApp
    if (showDAppDialog.value) {

        val context = LocalContext.current

        val tgUrl = textInput

        if (!tgUrl.startsWith("https://")) {
            Toast.makeText(context,"Please Enter DApp Launch URL", Toast.LENGTH_SHORT).show()
            showDAppDialog.value = false
            return
        }

        val config = DAppLaunchWParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .url(tgUrl)
            .bridgetProvider(WalletBridgeProviderImpl(context = context, owner = lifecycleOwner, dappUrl = tgUrl))
            .onDismissListener {
                showDAppDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)?.also(complete) ?: run {
                showDAppDialog.value = false
            }
        }
    }


    // mini app 对话框内容
    if (showDialog.value) {

        val context = LocalContext.current

        val tgUrl = textInput

        if (!tgUrl.startsWith("https://")) {
            Toast.makeText(context,"Please Enter Telegram WebApp Launch URL", Toast.LENGTH_SHORT).show()
            showDialog.value = false
            return
        }

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .url(tgUrl)
            .isLaunchUrl(true)
            .onDismissListener {
                showDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)?.also(complete) ?: run {
                showDialog.value = false
            }
        }
    }
}
@Composable
fun AppMasterButton(context:Context,lifecycleOwner: LifecycleOwner) {
    Button(modifier = Modifier.width(250.dp), onClick = {

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .miniAppId("2lv8dp7JjF2AU0iEk2rMYUaySjU")
            .onDismissListener {
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)
        }
    }) {
        Text(text = "AppMaster")
    }
}

@Composable
fun MarketPlaceButton(context:Context,lifecycleOwner: LifecycleOwner) {
    Button(modifier = Modifier.width(250.dp), onClick = {

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .startParam(uriMarketPlace.query)
            .miniAppId("10")
            .onDismissListener {
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)
        }
    }) {
        Text(text = "MarketPlace")
    }
}
