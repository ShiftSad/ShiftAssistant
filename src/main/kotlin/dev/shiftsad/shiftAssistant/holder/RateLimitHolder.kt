package dev.shiftsad.shiftAssistant.holder

import dev.shiftsad.shiftAssistant.service.RateLimitService

object RateLimitHolder {
    private var rateLimitService: RateLimitService? = null
    
    fun set(service: RateLimitService) {
        rateLimitService = service
    }
    
    fun get(): RateLimitService {
        return rateLimitService ?: throw IllegalStateException("RateLimitService not initialized")
    }
    
    fun isInitialized(): Boolean {
        return rateLimitService != null
    }
}
