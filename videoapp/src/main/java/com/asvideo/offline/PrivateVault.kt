package com.asvideo.offline

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypted copies only: the user's source video is never changed or deleted. */
class PrivateVault(private val context: Context) {
    private val directory = File(context.filesDir, "private_videos").apply { mkdirs() }
    private val index get() = File(directory, "index.json")
    data class Entry(val id: String, val name: String, val size: Long)
    fun entries(): List<Entry> {
        val array = if (index.exists()) JSONArray(index.readText()) else JSONArray()
        return (0 until array.length()).map { n ->
            val o = array.getJSONObject(n)
            Entry(o.getString("id"), o.getString("name"), o.getLong("size"))
        }
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("as_video_vault", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("as_video_vault", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun importVideo(uri: Uri, name: String): Entry {
        val id = UUID.randomUUID().toString()
        val file = File(directory, "$id.enc")
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            var count = 0L
            DataOutputStream(file.outputStream()).use { output ->
                output.writeInt(cipher.iv.size); output.write(cipher.iv)
                CipherOutputStream(output, cipher).use { encrypted ->
                    requireNotNull(context.contentResolver.openInputStream(uri)).use { input -> count = input.copyTo(encrypted) }
                }
            }
            val entry = Entry(id, name, count)
            val all = entries() + entry
            val array = JSONArray()
            all.forEach { e -> array.put(JSONObject().put("id", e.id).put("name", e.name).put("size", e.size)) }
            val temporary = File(directory, "index.tmp").apply { writeText(array.toString()) }
            check(temporary.renameTo(index))
            return entry
        } catch (error: Exception) { file.delete(); throw error }
    }
    fun open(entry: Entry): Uri {
        clearCache()
        val plain = File(context.cacheDir, "vault_playback.mp4")
        try {
            DataInputStream(File(directory, "${entry.id}.enc").inputStream()).use { input ->
                val size = input.readInt(); require(size in 12..32)
                val iv = ByteArray(size); input.readFully(iv)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
                CipherInputStream(input, cipher).use { decrypted -> plain.outputStream().use { decrypted.copyTo(it) } }
            }
            return Uri.fromFile(plain)
        } catch (error: Exception) { plain.delete(); throw error }
    }
    fun clearCache() { File(context.cacheDir, "vault_playback.mp4").delete() }
}
