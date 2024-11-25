package io.openweb3.sample.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import io.openweb3.plugins.openplatform.miniapp.IMiniApp
import io.openweb3.plugins.PLUGIN_OPEN_PLATFORM
import io.openweb3.plugins.PluginsManager
import io.openweb3.plugins.openplatform.OpenPlatformPlugin
import io.openweb3.plugins.openplatform.miniapp.WebAppLaunchWithParentParameters
import io.openweb3.sample.MyApplication
import io.openweb3.sample.R
import io.openweb3.sample.utils.AndroidUtils
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(
        PLUGIN_OPEN_PLATFORM
    )!!
    private val miniAppService = openPlatformPlugin.getMiniAppService()

    companion object {
        fun newInstance() = ChatFragment()
    }

    private val viewModel: MainViewModel by viewModels()
    private var miniApp: IMiniApp? = null

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(miniApp==null || true==miniApp?.requestDismiss()) {
                requireActivity().finish()
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)

        MyApplication.sendMessageLiveData.observe(this) {
            it?.also {
                sendNewMessage(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun sendNewMessage(content: String) {
        view?.findViewById<LinearLayout>(R.id.chatContent)?.addView( TextView(context).apply { text= content } )
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.statusBar).also {
            val layoutParams = it.layoutParams
            layoutParams.height = AndroidUtils.statusBarHeight
            it.layoutParams = layoutParams
        }

        view.findViewById<View>(R.id.toolbar).setOnClickListener {
            requireActivity().onBackPressed()
        }
        view.findViewById<View>(R.id.btnSend).setOnClickListener {
            val input = view.findViewById<EditText>(R.id.inputMessage)
            val inputContent = input.text.trim().toString()
            sendNewMessage(inputContent)
            input.setText("")
        }

        view.findViewById<Button>(R.id.launchMiniApp).setOnClickListener {

            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                0
            ).apply {
                bottomToTop = R.id.keyboardPanel
                leftToLeft = -1
                rightToRight = -1
                topToTop = R.id.statusBar
            }

            hideSoftInput(view.findViewById(R.id.inputMessage))

            val config = WebAppLaunchWithParentParameters.Builder()
                .owner(this)
                .context(requireContext())
                .botName("wallet")
                .miniAppName("wallet")
                .parentView(view.findViewById<ConstraintLayout>(R.id.chat))
                .layoutParams(layoutParams)
                .isLocal(true)
                .onDismissListener {
                    miniApp = null
                    view.findViewById<Button>(R.id.launchMiniApp).text = "Wallet"
                }
                .build()

            lifecycleScope.launch {
                miniApp = miniAppService.launch(config)
            }

            view.findViewById<Button>(R.id.launchMiniApp).text = "Close"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    private fun hideSoftInput(editText: EditText) {
        // 清除焦点并隐藏软键盘
        editText.clearFocus()
        try {
            val imm =
                editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (!imm.isActive()) {
                return
            }
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}