package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class JobStatus(
    val id: String,
    val image: String,
    val status: String,
    val durationSeconds: Long
)

@Composable
fun JobPipelineView() {
    var jobs by remember { mutableStateOf(emptyList<JobStatus>()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (isActive) {
            try {
                val url = URL("http://localhost:8080/api/jobs")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                
                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    jobs = Json { ignoreUnknownKeys = true }.decodeFromString(response)
                    errorMessage = ""
                } else {
                    errorMessage = "Failed to fetch jobs: $responseCode"
                }
            } catch (e: Exception) {
                errorMessage = "Error fetching jobs: ${e.message}"
            }
            delay(5000) // Poll every 5 seconds
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Job Pipeline", style = MaterialTheme.typography.h6)
        
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("ID", modifier = Modifier.weight(1f), style = MaterialTheme.typography.subtitle2)
            Text("Image", modifier = Modifier.weight(2f), style = MaterialTheme.typography.subtitle2)
            Text("Status", modifier = Modifier.weight(1f), style = MaterialTheme.typography.subtitle2)
            Text("Duration (s)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.subtitle2)
        }
        Divider()
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(jobs) { job ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(job.id, modifier = Modifier.weight(1f))
                    Text(job.image, modifier = Modifier.weight(2f))
                    Text(job.status, modifier = Modifier.weight(1f), color = when(job.status) {
                        "Running" -> MaterialTheme.colors.primary
                        "Succeeded" -> MaterialTheme.colors.secondary
                        "Failed" -> MaterialTheme.colors.error
                        else -> MaterialTheme.colors.onSurface
                    })
                    Text("${job.durationSeconds}s", modifier = Modifier.weight(1f))
                }
                Divider()
            }
        }
    }
}
