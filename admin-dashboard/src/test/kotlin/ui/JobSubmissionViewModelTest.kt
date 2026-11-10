package ui

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JobSubmissionViewModelTest {

    @Test
    fun `submitJob with invalid url should report error`() = runBlocking {
        val viewModel = JobSubmissionViewModel()
        viewModel.submitJob("ubuntu", "echo hello", "gpu", "http://invalid-endpoint-that-does-not-exist:8080/api/jobs")
        
        val status = viewModel.statusMessage.first()
        assertTrue(status.startsWith("Error"), "Expected error status due to connection failure")
    }
}
