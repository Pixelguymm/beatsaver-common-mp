/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
* File has been changed to tag requests via micrometer which is broken under normal use
*/

package io.beatmaps.common

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.call
import io.ktor.server.http.content.resolveResource
import io.ktor.server.response.ApplicationSendPipeline
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.coroutineScope

/**
 * Status pages feature that handles exceptions and status codes. Useful to configure default error pages.
 */
class StatusPagesCustom(config: Configuration) {
    private val exceptions = HashMap(config.exceptions)
    private val statuses = HashMap(config.statuses)

    /**
     * Status pages feature config
     */
    class Configuration {
        /**
         * Exception handlers map by exception class
         */
        val exceptions: MutableMap<Class<*>, suspend PipelineContext<*, ApplicationCall>.(Throwable) -> Unit> =
            mutableMapOf()

        /**
         * Status handlers by status code
         */
        val statuses: MutableMap<HttpStatusCode, suspend PipelineContext<*, ApplicationCall>.(HttpStatusCode) -> Unit> =
            mutableMapOf()

        /**
         * Register exception [handler] for exception type [T] and it's children
         */
        inline fun <reified T : Throwable> exception(
            noinline handler: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
        ): Unit =
            exception(T::class.java, handler)

        /**
         * Register exception [handler] for exception class [klass] and it's children
         */
        fun <T : Throwable> exception(
            klass: Class<T>,
            handler: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit
        ) {
            @Suppress("UNCHECKED_CAST")
            val cast =
                handler as suspend PipelineContext<*, ApplicationCall>.(Throwable) -> Unit

            exceptions[klass] = cast
        }

        /**
         * Register status [handler] for [status] code
         */
        fun status(
            vararg status: HttpStatusCode,
            handler: suspend PipelineContext<*, ApplicationCall>.(HttpStatusCode) -> Unit
        ) {
            status.forEach {
                statuses[it] = handler
            }
        }
    }

    private suspend fun interceptResponse(context: PipelineContext<*, ApplicationCall>, message: Any) {
        val call = context.call
        if (call.attributes.contains(key)) return

        val status = when (message) {
            is OutgoingContent -> message.status
            is HttpStatusCode -> message
            else -> null
        }
        if (status != null) {
            val handler = statuses[status]
            if (handler != null) {
                call.attributes.put(key, this@StatusPagesCustom)
                context.handler(status)
                finishIfResponseSent(context)
            }
        }
    }

    private fun finishIfResponseSent(context: PipelineContext<*, ApplicationCall>) {
        if (context.call.response.status() != null) {
            context.finish()
        }
    }

    private suspend fun interceptCall(context: PipelineContext<Unit, ApplicationCall>) {
        try {
            coroutineScope {
                context.proceed()
            }
        } catch (exception: Throwable) {
            context.call.tag("throwable", exception::class.qualifiedName ?: "n/a")

            val handler = findHandlerByType(exception.javaClass)
            if (handler != null && context.call.response.status() == null) {
                context.handler(exception)
                finishIfResponseSent(context)
            } else {
                throw exception
            }
        }
    }

    private fun findHandlerByType(clazz: Class<*>): HandlerFunction? {
        exceptions[clazz]?.let { return it }
        clazz.superclass?.let {
            findHandlerByType(it)?.let { found -> return found }
        }
        clazz.interfaces.forEach {
            findHandlerByType(it)?.let { found -> return found }
        }
        return null
    }

    /**
     * Feature installation object
     */
    companion object Feature : BaseApplicationPlugin<ApplicationCallPipeline, Configuration, StatusPagesCustom> {
        override val key: AttributeKey<StatusPagesCustom> = AttributeKey("Status Pages")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): StatusPagesCustom {
            val configuration = Configuration().apply(configure)
            val feature = StatusPagesCustom(configuration)
            if (feature.statuses.isNotEmpty()) {
                pipeline.sendPipeline.intercept(ApplicationSendPipeline.After) { message ->
                    feature.interceptResponse(this, message)
                }
            }
            if (feature.exceptions.isNotEmpty()) {
                pipeline.intercept(ApplicationCallPipeline.Monitoring) {
                    feature.interceptCall(this)
                }
            }
            return feature
        }
    }
}

/**
 * Register a status page file(s) using [filePattern] for multiple status [code] list
 * @param code vararg list of status codes handled by this configuration
 * @param filePattern path to status file with optional `#` character(s) that will be replaced with numeric status code
 */
fun StatusPagesCustom.Configuration.statusFile(vararg code: HttpStatusCode, filePattern: String) {
    status(*code) { status ->
        val path = filePattern.replace("#", status.value.toString())
        val message = call.resolveResource(path)
        if (message == null) {
            call.respond(HttpStatusCode.InternalServerError)
        } else {
            call.response.status(status)
            call.respond(message)
        }
    }
}
private typealias HandlerFunction = suspend PipelineContext<Unit, ApplicationCall>.(Throwable) -> Unit
