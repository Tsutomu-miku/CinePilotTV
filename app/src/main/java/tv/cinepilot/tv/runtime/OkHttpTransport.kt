package tv.cinepilot.tv.runtime

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tv.cinepilot.core.protocol.HttpMethod
import tv.cinepilot.core.protocol.HttpTransport
import tv.cinepilot.core.protocol.MediaServerAddress
import tv.cinepilot.core.protocol.ProtocolRequest
import tv.cinepilot.core.protocol.ProtocolResponse

class OkHttpTransport(
    private val client: OkHttpClient,
) : HttpTransport {
    @Throws(IOException::class)
    override fun send(address: MediaServerAddress, request: ProtocolRequest): ProtocolResponse {
        val builder = Request.Builder().url(request.url(address))
        for ((name, value) in request.headers()) {
            builder.header(name, value)
        }
        val body = (request.bodyJson() ?: "").toRequestBody(JSON_MEDIA_TYPE)
        when (request.method()) {
            HttpMethod.GET -> builder.get()
            HttpMethod.POST -> builder.post(body)
            HttpMethod.DELETE -> builder.delete()
        }
        client.newCall(builder.build()).execute().use { response ->
            return ProtocolResponse(
                response.code,
                response.headers.names().associateWith { name ->
                    response.headers.values(name).joinToString(",")
                },
                response.body?.string().orEmpty(),
            )
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
