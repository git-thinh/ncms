package com.softmotions.ncms.atm

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Inject
import com.google.inject.Singleton
import com.softmotions.ncms.events.NcmsEventBus
import com.softmotions.ncms.events.UserDisconnectedEvent
import org.atmosphere.cache.UUIDBroadcasterCache
import org.atmosphere.client.TrackMessageSizeInterceptor
import org.atmosphere.config.service.AtmosphereHandlerService
import org.atmosphere.cpr.*
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor
import org.atmosphere.interceptor.BroadcastOnPostAtmosphereInterceptor
import org.atmosphere.interceptor.HeartbeatInterceptor
import org.atmosphere.interceptor.SuspendTrackerInterceptor
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Ncms admin UI websocket events handler.

 * @author Adamansky Anton (adamansky@gmail.com)
 */

@Singleton
@AtmosphereHandlerService(path = "/ws/adm/ui",
        interceptors = arrayOf(
                AtmosphereResourceLifecycleInterceptor::class,
                TrackMessageSizeInterceptor::class,
                SuspendTrackerInterceptor::class,
                BroadcastOnPostAtmosphereInterceptor::class,
                HeartbeatInterceptor::class),
        broadcasterCache = UUIDBroadcasterCache::class,
        listeners = arrayOf(AdminUIWS.RSEvents::class))
@JvmSuppressWildcards
open class AdminUIWS
@Inject
constructor(private val mapper: ObjectMapper,
            private val resourceFactory: AtmosphereResourceFactory,
            private val ebus: NcmsEventBus) : OnMessageAtmosphereHandler<String>() {


    companion object {
        private val log = LoggerFactory.getLogger(AdminUIWS::class.java)
    }

    private val ruuid2User = HashMap<String, String>()

    private val user2ruuids = HashMap<String, MutableSet<String>>()

    private val lock = ReentrantLock()

    init {
        log.info("AdminUIWS instantiated")
        ebus.register(this);
    }

    override fun onOpen(resource: AtmosphereResource) = register(resource)

    override fun onDisconnect(response: AtmosphereResponse, event: AtmosphereResourceEvent) = terminate(event.resource)

    override fun onTimeout(response: AtmosphereResponse, event: AtmosphereResourceEvent) = terminate(event.resource)

    private fun register(resource: AtmosphereResource) {
        val uuid = resource.uuid()
        val user = resource.request.wrappedRequest().userPrincipal?.name ?: return run {
            log.warn("Unauthenticated user within 'ws/admin/ui' atmosphere channel. UUID: {}", uuid)
        }
        log.info("Register atmosphere resource: {} for user: {}", uuid, user)
        lock.withLock {
            ruuid2User[uuid] = user
            val uuids = user2ruuids.getOrPut(user, {
                HashSet<String>()
            })
            uuids += uuid
        }
    }

    private fun terminate(resource: AtmosphereResource) {
        val uuid = resource.uuid()
        log.info("Timeout/Disconnected uuid={}", uuid)
        val user = lock.withLock {
            val user = ruuid2User.remove(uuid) ?: return@withLock null
            val uuids = user2ruuids[user] ?: return@withLock user
            uuids.remove(uuid)
            if (!uuids.isEmpty()) {
                log.info("Found live uuids for user: {} uuids: {}", user, uuids)
                null
            } else {
                user2ruuids.remove(user)
                user
            }
        } ?: return
        log.info("User: {} disconnected", user)
        ebus.fire(UserDisconnectedEvent(user, this))
    }

    @Throws(IOException::class)
    override fun onMessage(response: AtmosphereResponse,
                           data: String,
                           event: AtmosphereResourceEvent) {
        val msg = WSMessage(mapper, data)
        val resp = mapper.createObjectNode()
        resp.put("message", "Response for " + msg.uuid)
        response.writer.write(WSMessage(mapper, resp).toString())
    }

    @Singleton
    @JvmSuppressWildcards
    open class RSEvents
    @Inject
    constructor(private val aws: AdminUIWS) : AtmosphereResourceEventListenerAdapter() {

        override fun onClose(event: AtmosphereResourceEvent) {
            aws.terminate(event.resource)
        }

        override fun onDisconnect(event: AtmosphereResourceEvent) {
            aws.terminate(event.resource)
        }

        override fun onThrowable(event: AtmosphereResourceEvent) {
            aws.terminate(event.resource)
        }
    }
}
