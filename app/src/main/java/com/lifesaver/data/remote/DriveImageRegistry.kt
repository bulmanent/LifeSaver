package com.lifesaver.data.remote

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

object DriveImageRegistry {

    private val registered = AtomicBoolean(false)

    fun register(context: Context, service: GoogleSheetsDriveService) {
        if (registered.compareAndSet(false, true)) {
            Glide.get(context).registry.prepend(
                DriveImageRef::class.java,
                InputStream::class.java,
                object : ModelLoaderFactory<DriveImageRef, InputStream> {
                    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<DriveImageRef, InputStream> {
                        return DriveImageModelLoader(service)
                    }

                    override fun teardown() = Unit
                }
            )
        }
    }
}
