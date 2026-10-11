package com.perol.asdpl.pixivez.networks

import okhttp3.Response
import java.io.IOException

/** OkHttp async calls report IOExceptions to Retrofit; unchecked refresh errors crash its worker. */
internal fun refreshAuthentication(response: Response, refresh: () -> Unit) {
    try {
        refresh()
    } catch (error: IOException) {
        throw error
    } catch (error: Exception) {
        throw IOException("Identity refresh failed: ${error.message}", error)
    } finally {
        response.close()
    }
}
