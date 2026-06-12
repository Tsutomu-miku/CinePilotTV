package tv.cinepilot.tv.offline

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import tv.cinepilot.core.protocol.OfflineRepository
import java.io.File

/**
 * Persists the in-memory [OfflineRepository] to a private JSON file so queue
 * state, progress, and per-server quotas survive process death.
 *
 * <p>The file format is intentionally simple (no data class / kotlinx-serialization
 * dependency) and is written atomically via rename. Load failures simply yield
 * an empty repository; the Android downloader owns the authoritative Media3
 * download index and will reconcile state on next boot anyway.
 */
internal object OfflineRepositoryStore {

    private const val FILE_NAME = "cinepilot_offline_repository_v1.json"

    fun load(context: Context): OfflineRepository {
        val file = file(context)
        val repo = OfflineRepository()
        if (!file.isFile) return repo
        runCatching {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            val quotas = root.optJSONObject("quotas")
            if (quotas != null) {
                for (serverId in quotas.keys()) {
                    val q = quotas.getJSONObject(serverId)
                    repo.setQuota(
                        serverId,
                        OfflineRepository.Quota(
                            q.optLong("bytes", 0L),
                            q.optInt("items", 0),
                        ),
                    )
                }
            }
            val entries = root.optJSONArray("entries") ?: return@runCatching
            for (i in 0 until entries.length()) {
                val e = entries.getJSONObject(i)
                val entry = OfflineRepository.Entry(
                    e.getString("serverId"),
                    e.getString("itemId"),
                    e.optString("itemName", ""),
                    e.optInt("quality", 0),
                    e.optLong("mediaSourceIdHash", 0L),
                ).apply {
                    val stateName = e.optString("state", OfflineRepository.State.QUEUED.name)
                    setState(runCatching { OfflineRepository.State.valueOf(stateName) }
                        .getOrDefault(OfflineRepository.State.QUEUED))
                    setBytes(e.optLong("bytesDownloaded", 0L), e.optLong("bytesTotal", 0L))
                    setLocalContentId(e.optLong("localContentId", 0L))
                    setErrorMessage(e.optString("errorMessage", null))
                }
                // Re-insert without going through the quota gate: these are existing
                // entries the user already committed to.
                repo.enqueueExisting(entry)
            }
        }
        return repo
    }

    fun save(context: Context, repo: OfflineRepository) {
        val file = file(context)
        runCatching {
            val root = JSONObject()
            val entriesJson = JSONArray()
            for (entry in repo.allEntries()) {
                entriesJson.put(JSONObject().apply {
                    put("serverId", entry.serverId())
                    put("itemId", entry.itemId())
                    put("itemName", entry.itemName())
                    put("quality", entry.quality())
                    put("mediaSourceIdHash", entry.mediaSourceIdHash())
                    put("state", entry.state().name)
                    put("bytesDownloaded", entry.bytesDownloaded())
                    put("bytesTotal", entry.bytesTotal())
                    put("localContentId", entry.localContentId())
                    put("errorMessage", entry.errorMessage())
                })
            }
            root.put("entries", entriesJson)
            // (quotas intentionally not persisted — servers push these on every session)
            val tmp = File(file.absolutePath + ".tmp")
            tmp.writeText(root.toString(), Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                tmp.delete()
                file.writeText(root.toString(), Charsets.UTF_8)
            }
        }
    }

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)
}
