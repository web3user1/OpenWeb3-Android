package io.openweb3.sample.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Point
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
import android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import io.openweb3.plugins.core.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min


/**
 * Util class for managing themes.
 */
@SuppressLint("StaticFieldLeak")
object AndroidUtils {
    // preference key
    const val APPLICATION_THEME_KEY = "APPLICATION_THEME_KEY"

    // the theme possible raw
    private const val SYSTEM_THEME_VALUE = "system"
    private const val THEME_DARK_VALUE = "dark"
    private const val THEME_LIGHT_VALUE = "light"
    private const val THEME_BLACK_VALUE = "black"

    // The default theme
    private const val DEFAULT_THEME = SYSTEM_THEME_VALUE

    private var currentTheme = AtomicReference<String>(null)

    private val mColorByAttr = HashMap<Int, Int>()

    @SuppressLint("StaticFieldLeak")
    private lateinit var mContext: Context

    private var vibrator: Vibrator? = null

    fun getVibrator(): Vibrator? {
        if (vibrator == null) {
           vibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        return vibrator
    }

    var density: Float = 1f
        private set

    var displaySize = Point()
        private set

    var touchSlop = 0f

    val rectTmp = RectF()
    var isInMultiWindow = false
        private set

    var statusBarHeight = 0
        private set

    var navigationBarHeight = 0
        private set

    private var firstConfigurationWas: Boolean = true
        private set

    const val LIGHT_STATUS_BAR_OVERLAY = 0x0f000000
    const val DARK_STATUS_BAR_OVERLAY = 0x33000000

    fun checkAndroidTheme(context: Context, open: Boolean) {
        mContext = context
        context.setTheme(if (isSystemDarkTheme(context.resources) && open) R.style.Theme_TMessages_Dark else R.style.Theme_TMessages_Light)
    }

    /**
     * @return true if current theme is System
     */
    fun isSystemTheme(context: Context): Boolean {
        val theme = getApplicationTheme(context)
        return theme == SYSTEM_THEME_VALUE
    }

    /**
     * @return true if current theme is Light or current theme is System and system theme is light
     */
    fun isLightTheme(): Boolean {
        val theme = getApplicationTheme(mContext)
        return theme == THEME_LIGHT_VALUE ||
                (theme == SYSTEM_THEME_VALUE && !isSystemDarkTheme(mContext.resources))
    }

    /**
     * Provides the selected application theme.
     *
     * @param context the context
     * @return the selected application theme
     */
    private fun getApplicationTheme(context: Context): String {
        val currentTheme = currentTheme.get()
        return if (currentTheme == null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            var themeFromPref = prefs.getString(APPLICATION_THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
            if (themeFromPref == "status") {
                // Migrate to the default theme
                themeFromPref = DEFAULT_THEME
                prefs.edit { putString(APPLICATION_THEME_KEY, DEFAULT_THEME) }
            }
            AndroidUtils.currentTheme.set(themeFromPref)
            themeFromPref
        } else {
            currentTheme
        }
    }

    /**
     * @return true if system theme is dark
     */
    private fun isSystemDarkTheme(resources: Resources): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Translates color attributes to colors.
     *
     * @param c Context
     * @param colorAttribute Color Attribute
     * @return Requested Color
     */
    @ColorInt
    fun getColor(c: Context, @AttrRes colorAttribute: Int): Int {
        return mColorByAttr.getOrPut(colorAttribute) {
            try {
                val color = TypedValue()
                c.theme.resolveAttribute(colorAttribute, color, true)
                color.data
            } catch (e: Exception) {
                Log.e("ThemeUtil", "Unable to get color")
                ContextCompat.getColor(c, android.R.color.holo_red_dark)
            }
        }
    }

    fun getAttribute(c: Context, @AttrRes attribute: Int): TypedValue? {
        try {
            val typedValue = TypedValue()
            c.theme.resolveAttribute(attribute, typedValue, true)
            return typedValue
        } catch (e: Exception) {
            Log.e("ThemeUtil","Unable to get color")
        }
        return null
    }

    fun dpf2(value: Float): Float {
        return if (value == 0f) {
            0f
        } else density * value
    }

    fun dp(value: Float): Int {
        return if (value.toInt() == 0) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    fun dp(value: Int): Int {
        return if (value == 0) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    fun isTablet(context: Context): Boolean {
        return context.applicationContext != null &&
                context.applicationContext.resources.getBoolean(R.bool.isTablet)
    }

    fun isSmallTablet(): Boolean {
        val minSide: Float = min(
            displaySize.x,
            displaySize.y
        ) / density
        return minSide <= 690
    }

    fun fillStatusBarHeight(context: Context, force: Boolean) {
        if ( statusBarHeight > 0 && !force) {
            return
        }
        statusBarHeight = getStatusBarHeight(context)
        navigationBarHeight = getNavigationBarHeight(context)
    }

    fun getActionBarSize(context: Context): Int {
        var sizeValue = 0
        val styledAttributes: TypedArray = context.theme.obtainStyledAttributes(
            intArrayOf(android.R.attr.actionBarSize)
        )
        sizeValue = styledAttributes.getDimension(0, 0f).toInt()
        styledAttributes.recycle()
        return dp(sizeValue)
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun setLightNavigationBar(view: View?, enable: Boolean) {
        if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = view.windowInsetsController

            if (windowInsetsController != null) {

                if (enable) {
                    windowInsetsController.setSystemBarsAppearance(
                        APPEARANCE_LIGHT_NAVIGATION_BARS,
                        APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    windowInsetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_NAVIGATION_BARS)
                }
            }
        }
        else if (view != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            var flags = view.systemUiVisibility
            @Suppress("DEPRECATION")
            if (flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR > 0 != enable) {
                flags = if (enable) {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                } else {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                }
                view.systemUiVisibility = flags
            }
        }
    }

    fun setLightNavigationBar(window: Window?, enable: Boolean) {
        if (window != null) {
            setLightNavigationBar(window.decorView, enable)
        }
    }

    fun setLightStatusBar(view: View, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = view.windowInsetsController
            if (windowInsetsController != null) {
                if (enable) {
                    windowInsetsController.setSystemBarsAppearance(
                        APPEARANCE_LIGHT_STATUS_BARS,
                        APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    windowInsetsController.setSystemBarsAppearance(0, APPEARANCE_LIGHT_STATUS_BARS)
                }
            }
        } else {
            var flags = view.systemUiVisibility
            if (enable) {
                if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR == 0) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    view.systemUiVisibility = flags
                }
            } else {
                if (flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0) {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    view.systemUiVisibility = flags
                }
            }
        }
    }
}
