package com.wizlabs.billbox.drive

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class DriveAuthException(message: String) : IOException(message)
class DriveHttpException(val code: Int, message: String) : IOException(message)

data class RemoteBill(val jsonId: String, val folderId: String?, val billId: String, val updatedAt: Long)

/**
 * A small Google Drive v3 REST client. It only ever sees files this app created
 * (the `drive.file` scope), so it can't read anything else in the user's Drive.
 */
class DriveApi(private val token: String) {

    fun about(): Pair<String, String> {
        val o = json(get("$API/about?fields=user(emailAddress,displayName)"))
        val u = o.getJSONObject("user")
        return u.optString("emailAddress") to u.optString("displayName")
    }

    /** The visible "Wiz Bill Box" folder in My Drive; created on first use. */
    fun findOrCreateRoot(knownId: String?): String {
        if (knownId != null) {
            val ok = runCatching {
                val o = json(get("$API/files/$knownId?fields=id,trashed"))
                !o.optBoolean("trashed")
            }.getOrDefault(false)
            if (ok) return knownId
        }
        val q = "mimeType='$FOLDER' and trashed=false and appProperties has {key='wizBillBox' and value='root'}"
        val found = json(get("$API/files?q=${enc(q)}&fields=files(id)&spaces=drive")).optJSONArray("files")
        if (found != null && found.length() > 0) return found.getJSONObject(0).getString("id")
        return createFolder(ROOT_NAME, null, mapOf("wizBillBox" to "root"))
    }

    fun createFolder(name: String, parent: String?, props: Map<String, String> = emptyMap(), description: String? = null): String {
        val meta = JSONObject().put("name", name).put("mimeType", FOLDER)
        if (description != null) meta.put("description", description)
        if (parent != null) meta.put("parents", JSONArray().put(parent))
        if (props.isNotEmpty()) meta.put("appProperties", JSONObject(props))
        return json(send("POST", "$API/files?fields=id", meta.toString().toByteArray(), "application/json")).getString("id")
    }

    fun upload(
        name: String, mime: String, content: ByteArray, parent: String,
        props: Map<String, String> = emptyMap(), description: String? = null,
    ): String {
        val meta = JSONObject().put("name", name).put("parents", JSONArray().put(parent))
        if (props.isNotEmpty()) meta.put("appProperties", JSONObject(props))
        if (description != null) meta.put("description", description)
        return multipart("POST", "$UPLOAD/files?uploadType=multipart&fields=id", meta, mime, content).getString("id")
    }

    fun update(id: String, name: String?, mime: String, content: ByteArray, props: Map<String, String> = emptyMap()) {
        val meta = JSONObject()
        if (name != null) meta.put("name", name)
        if (props.isNotEmpty()) meta.put("appProperties", JSONObject(props))
        multipart("PATCH", "$UPLOAD/files/$id?uploadType=multipart&fields=id", meta, mime, content)
    }

    fun rename(id: String, name: String, description: String? = null) {
        val meta = JSONObject().put("name", name)
        if (description != null) meta.put("description", description)
        send("PATCH", "$API/files/$id?fields=id", meta.toString().toByteArray(), "application/json")
    }

    /** Moves a file or folder to the Drive bin (recoverable for 30 days), never a hard delete. */
    fun trash(id: String) {
        try {
            send("PATCH", "$API/files/$id?fields=id", """{"trashed":true}""".toByteArray(), "application/json")
        } catch (e: DriveHttpException) {
            if (e.code != 404) throw e // already gone
        }
    }

    fun exists(id: String): Boolean = try {
        !json(get("$API/files/$id?fields=id,trashed")).optBoolean("trashed")
    } catch (e: DriveHttpException) {
        if (e.code == 404) false else throw e
    }

    fun download(id: String): ByteArray = get("$API/files/$id?alt=media")

    fun downloadTo(id: String, target: File) {
        val tmp = File(target.parentFile, target.name + ".part")
        open("GET", "$API/files/$id?alt=media").let { c ->
            check(c)
            c.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
        }
        if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
    }

    /** Every bill.json this app has stored in the user's Drive, across all their phones. */
    fun listBills(): List<RemoteBill> {
        val q = "appProperties has {key='wizKind' and value='bill'} and trashed=false"
        val out = mutableListOf<RemoteBill>()
        var page: String? = null
        do {
            val url = "$API/files?q=${enc(q)}&spaces=drive&pageSize=1000" +
                "&fields=${enc("nextPageToken,files(id,parents,appProperties)")}" +
                (page?.let { "&pageToken=${enc(it)}" } ?: "")
            val o = json(get(url))
            val files = o.optJSONArray("files") ?: JSONArray()
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val p = f.optJSONObject("appProperties") ?: continue
                val billId = p.optString("billId")
                if (billId.isEmpty()) continue
                out += RemoteBill(
                    jsonId = f.getString("id"),
                    folderId = f.optJSONArray("parents")?.optString(0),
                    billId = billId,
                    updatedAt = p.optString("updatedAt").toLongOrNull() ?: 0L,
                )
            }
            page = o.optString("nextPageToken").ifEmpty { null }
        } while (page != null)
        return out
    }

    // ---- HTTP plumbing ----

    private fun multipart(method: String, url: String, meta: JSONObject, mime: String, content: ByteArray): JSONObject {
        val boundary = "wizbillbox" + System.nanoTime()
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray())
            write(meta.toString().toByteArray())
            write("\r\n--$boundary\r\nContent-Type: $mime\r\n\r\n".toByteArray())
            write(content)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        return json(send(method, url, body, "multipart/related; boundary=$boundary"))
    }

    private fun get(url: String): ByteArray = open("GET", url).let { check(it); it.inputStream.use(InputStream::readBytes) }

    private fun send(method: String, url: String, body: ByteArray, type: String): ByteArray {
        val c = open(method, url)
        c.doOutput = true
        c.setRequestProperty("Content-Type", type)
        c.setFixedLengthStreamingMode(body.size)
        c.outputStream.use { it.write(body) }
        check(c)
        return c.inputStream.use(InputStream::readBytes)
    }

    private fun open(method: String, url: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        if (method == "PATCH") {
            // HttpURLConnection doesn't speak PATCH; Google APIs accept this override.
            c.requestMethod = "POST"
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        } else {
            c.requestMethod = method
        }
        c.connectTimeout = 20_000
        c.readTimeout = 60_000
        c.setRequestProperty("Authorization", "Bearer $token")
        return c
    }

    private fun check(c: HttpURLConnection) {
        val code = c.responseCode
        if (code in 200..299) return
        val err = runCatching { c.errorStream?.use { it.readBytes().decodeToString() } }.getOrNull().orEmpty()
        if (code == 401) throw DriveAuthException("Google sign-in expired")
        val msg = runCatching { JSONObject(err).getJSONObject("error").getString("message") }.getOrNull() ?: "HTTP $code"
        throw DriveHttpException(code, msg)
    }

    private fun json(bytes: ByteArray) = JSONObject(bytes.decodeToString())
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    companion object {
        const val ROOT_NAME = "Wiz Bill Box"
        const val FOLDER = "application/vnd.google-apps.folder"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    }
}
