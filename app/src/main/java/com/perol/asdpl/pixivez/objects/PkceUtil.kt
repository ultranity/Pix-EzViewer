// Copied from https://github.com/CeuiLiSA/Pixiv-Shaft/blob/77e0cb739cf502b7453000ff5738b79697ca9666/app/src/main/java/ceui/lisa/feature/PkceUtil.java (MIT)
// Converted to Kotlin

package com.perol.asdpl.pixivez.objects

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

object PkceUtil {
    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.encodeToString(
            codeVerifier,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}