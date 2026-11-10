package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class JobRequest(val image: String, val command: String, val tags: List<String>)

@Composable
fun JobSubmissionForm() {
    var image by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Submit Job", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = image,
            onValueChange = { image = it },
            label = { Text("Docker Image") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Command") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = tags,
            onValueChange = { tags = it },
            label = { Text("Target Node Tags (comma separated)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        val request = JobRequest(
                            image = image,
                            command = command,
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        val url = URL("http://localhost:8080/api/jobs")
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
                            statusMessage = "Job submitted successfully!"
                            image = ""
                            command = ""
                            tags = ""
                        } else {
                            statusMessage = "Failed to submit job: $responseCode"
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    }
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Submit")
        }
        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, modifier = Modifier.padding(top = 8.dp), color = if (statusMessage.startsWith("Error") || statusMessage.startsWith("Failed")) MaterialTheme.colors.error else MaterialTheme.colors.primary)
        }
    }
}
