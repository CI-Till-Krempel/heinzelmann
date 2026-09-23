package de.heinzelmann.server.jobs

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.jobRoutes(jobQueue: JobQueue, jobDispatcher: JobDispatcher) {
    route("/api/jobs") {
        post {
            val request = call.receive<JobSubmissionRequest>()
            val job = jobQueue.enqueue(request)
            call.respond(HttpStatusCode.Created, job)
        }

        get {
            val statusParam = call.request.queryParameters["status"]
            val filterStatus = statusParam?.let {
                runCatching { JobStatus.valueOf(it.uppercase()) }.getOrNull()
            }
            val jobs = jobQueue.listJobs(filterStatus)
            call.respond(HttpStatusCode.OK, jobs)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing job ID")
            val job = jobQueue.getJob(id) ?: return@get call.respond(HttpStatusCode.NotFound, "Job not found")
            call.respond(HttpStatusCode.OK, job)
        }

        post("/{id}/status") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing job ID")
            val request = call.receive<JobStatusUpdateRequest>()
            val updated = jobQueue.updateJobStatus(id, request.status, request.result)
                ?: return@post call.respond(HttpStatusCode.NotFound, "Job not found")
            call.respond(HttpStatusCode.OK, updated)
        }

        post("/dispatch") {
            val assignments = jobDispatcher.dispatchNextPendingJobs()
            call.respond(HttpStatusCode.OK, assignments)
        }

        post("/recover") {
            val recovered = jobDispatcher.checkAndRecoverFailures()
            call.respond(HttpStatusCode.OK, recovered)
        }
    }
}
