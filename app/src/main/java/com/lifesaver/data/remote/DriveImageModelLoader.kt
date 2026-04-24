package com.lifesaver.data.remote

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

class DriveImageModelLoader(
    private val service: GoogleSheetsDriveService
) : ModelLoader<DriveImageRef, InputStream> {

    override fun buildLoadData(
        model: DriveImageRef,
        width: Int,
        height: Int,
        options: Options
    ): LoadData<InputStream> {
        return LoadData(ObjectKey(model.fileId), Fetcher(service, model))
    }

    override fun handles(model: DriveImageRef): Boolean = model.fileId.isNotBlank()

    private class Fetcher(
        private val service: GoogleSheetsDriveService,
        private val model: DriveImageRef
    ) : DataFetcher<InputStream> {

        private var stream: InputStream? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
            try {
                stream = service.openDriveFileStream(model.fileId)
                callback.onDataReady(stream)
            } catch (t: Throwable) {
                callback.onLoadFailed(t)
            }
        }

        override fun cleanup() {
            stream?.close()
            stream = null
        }

        override fun cancel() = Unit

        override fun getDataClass(): Class<InputStream> = InputStream::class.java

        override fun getDataSource(): DataSource = DataSource.REMOTE
    }
}
