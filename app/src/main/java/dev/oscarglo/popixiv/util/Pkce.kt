package dev.oscarglo.popixiv.util

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

object Pkce {
    class Item(val verify: String, val challenge: String)

    private var pkceItem: Item? = null

    fun get(): Item {
        if (pkceItem == null) {
            val verify: String = generateCodeVerifier()
            val challenge: String = generateCodeChallenge(verify)
            pkceItem = Item(verify, challenge)
        }
        return pkceItem!!
    }

    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val codeVerifier = ByteArray(32)
        secureRandom.nextBytes(codeVerifier)
        return Base64.encodeToString(
            codeVerifier,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(StandardCharsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes, 0, bytes.size)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}