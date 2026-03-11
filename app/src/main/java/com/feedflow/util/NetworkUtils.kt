package com.feedflow.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class CircuitState(
    val failureCount: Int = 0,
    val lastFailureTime: Long = 0,
    val isOpen: Boolean = false
)

object NetworkUtils {
    private val inFlightRequests = ConcurrentHashMap.newKeySet<String>()
    private val requestMutex = Mutex()
    private val circuitStates = ConcurrentHashMap<String, CircuitState>()
    private val activeRequests = AtomicInteger(0)

    const val MAX_CONCURRENT_REQUESTS = 10
    const val CIRCUIT_BREAKER_THRESHOLD = 5
    const val CIRCUIT_BREAKER_RESET_MS = 30_000L
    const val MAX_CACHE_SIZE = 100

    suspend fun <T> withRequestDeduplication(key: String, block: suspend () -> T): T {
        requestMutex.withLock {
            while (activeRequests.get() >= MAX_CONCURRENT_REQUESTS) {
                kotlinx.coroutines.delay(50)
            }
            while (inFlightRequests.contains(key)) {
                kotlinx.coroutines.delay(50)
            }
            inFlightRequests.add(key)
            activeRequests.incrementAndGet()
        }
        return try {
            block()
        } finally {
            inFlightRequests.remove(key)
            activeRequests.decrementAndGet()
        }
    }

    fun isCircuitOpen(serviceId: String): Boolean {
        val state = circuitStates[serviceId] ?: return false
        if (!state.isOpen) return false

        if (System.currentTimeMillis() - state.lastFailureTime > CIRCUIT_BREAKER_RESET_MS) {
            circuitStates[serviceId] = state.copy(isOpen = false, failureCount = 0)
            return false
        }
        return true
    }

    fun recordSuccess(serviceId: String) {
        circuitStates[serviceId] = CircuitState()
    }

    fun recordFailure(serviceId: String) {
        val current = circuitStates[serviceId] ?: CircuitState()
        val newCount = current.failureCount + 1
        val isOpen = newCount >= CIRCUIT_BREAKER_THRESHOLD
        circuitStates[serviceId] = CircuitState(
            failureCount = newCount,
            lastFailureTime = System.currentTimeMillis(),
            isOpen = isOpen
        )
    }

    fun <K, V> createBoundedCache(): MutableMap<K, V> {
        return object : LinkedHashMap<K, V>(MAX_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                return size > MAX_CACHE_SIZE
            }
        }
    }
}
