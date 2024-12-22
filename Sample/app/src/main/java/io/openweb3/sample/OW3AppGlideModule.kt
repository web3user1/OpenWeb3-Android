package io.openweb3.sample

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.module.AppGlideModule

@GlideModule
class OW3AppGlideModule : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val memoryCacheSizeBytes = 1024 * 1024 * 20
        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes.toLong()))
    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}