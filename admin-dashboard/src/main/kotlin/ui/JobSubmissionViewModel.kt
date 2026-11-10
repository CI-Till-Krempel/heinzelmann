package ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class JobSubmissionViewModel {
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    fun submitJob(image: String, command: String, tags: String, endpoint: String = "http://localhost:8080/api/jobs") {
        try {
            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val request = JobRequest(image = image, command = command, tags = tagList)
            
            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            val json = Json.encodeToString(request)
            connection.outputStream.use { os ->
                os.write(json.toByteArray())
            }
            
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                _statusMessage.update { "Job submitted successfully!" }
            } else {
                _statusMessage.update { "Failed to submit job: $responseCode" }
            }
        } catch (e: Exception) {
            _statusMessage.update { "Error: ${e.message}" }
        }
    }
}
