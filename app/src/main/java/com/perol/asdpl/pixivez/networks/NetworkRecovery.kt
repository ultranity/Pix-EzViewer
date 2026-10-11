package com.perol.asdpl.pixivez.networks

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The registration is removed when the view stops, including when it is destroyed. */
internal fun observeNetworkRecovery(owner: LifecycleOwner, context: Context, retry: () -> Unit) {
    val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
        as ConnectivityManager
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            callbackFlow {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) { trySend(Unit) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    manager.registerDefaultNetworkCallback(callback)
                } else {
                    manager.registerNetworkCallback(NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), callback)
                }
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }.collect {
                // get() may hold the cache monitor during DNS IO; never wait on it on Main.
                if (SniMode.current() == SniMode.ECH) {
                    withContext(Dispatchers.IO) { EchApiTransport.configs.onNetworkAvailable() }
                }
                retry()
            }
        }
    }
}
