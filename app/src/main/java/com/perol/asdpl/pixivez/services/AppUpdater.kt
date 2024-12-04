package com.perol.asdpl.pixivez.services

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Environment
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.perol.asdpl.pixivez.BuildConfig
import com.perol.asdpl.pixivez.R
import com.perol.asdpl.pixivez.base.MaterialDialogs
import com.perol.asdpl.pixivez.networks.ServiceFactory.gson
import com.perol.asdpl.pixivez.objects.ToastQ
import io.noties.markwon.Markwon
import kotlinx.serialization.SerialName
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.concurrent.thread

//ref: https://github.com/saikou-app/saikou/blob/9e157563b7c8850640999369ad578cb6e1d5af24/app/src/main/java/ani/saikou/others/AppUpdater.kt#L36
data class GithubResponse(
    @SerialName("html_url")
    val htmlUrl: String,
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val prerelease: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("published_at")
    val publishedAt: String,
    val body: String = "~",
    val assets: List<Asset>? = null
) {
    data class Asset(
        val name: String,
        val label: String,
        val size: Int,
        @SerialName("download_count")
        val downloadCount: Int,
        @SerialName("browser_download_url")
        val browserDownloadURL: String,
    )

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    }
}

object AppUpdater {
    //private val releaseURL = URL("https://github.com/ultranity/Pix-EzViewer/releases/latest")
    private val apiURL = URL("https://api.github.com/repos/ultranity/Pix-EzViewer/releases/latest")
    private lateinit var data: GithubResponse
    private fun isNewUpdateAvailable(): Boolean? = try {
        val response = apiURL.readText()
        data = gson.decodeFromString<GithubResponse>(response)
        val currentVersionName = BuildConfig.VERSION_NAME
        data.tagName != currentVersionName
    } catch (e: Exception) {
        null
    }

    fun checkUpgrade(activity: Activity, view: View) {
        if (isNetworkAvailable()) {
            thread {
                val newUpdateAvailable = isNewUpdateAvailable()

                activity.runOnUiThread {
                    if (newUpdateAvailable == null) {
                        ToastQ.post(R.string.update_failed)
                        return@runOnUiThread
                    }
                    // obtain an instance of Markwon
                    val markwon = Markwon.create(activity)
                    val title = activity.getString(
                        if (newUpdateAvailable) R.string.update_available
                        else R.string.no_update
                    ) + "|" + data.name + "|" + data.createdAt
                    MaterialDialogs(activity).show {
                        setTitle(title)
                        setMessage(markwon.toMarkdown(data.body))
                        setPositiveButton(R.string.download) { _, _ ->
                            Snackbar.make(
                                view,
                                activity.getString(R.string.update_now),
                                Snackbar.LENGTH_SHORT
                            ).setAction(R.string.download) {
                                requestDownload(activity)
                            }.show()
                        }
                        cancelButton()
                    }
                }
            }
        }
    }

    private fun requestDownload(activity: Activity) {
        val asset = data.assets!![0]
        val request = DownloadManager.Request(Uri.parse(asset.browserDownloadURL))
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        request.setTitle("Downloading " + asset.name)
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Pix-EzViewer ${data.name}"
            )
        downloadManager.enqueue(request)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            PxEZApp.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.state == NetworkInfo.State.CONNECTED
    }
}