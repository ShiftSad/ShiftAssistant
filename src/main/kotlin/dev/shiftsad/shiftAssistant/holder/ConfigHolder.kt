package dev.shiftsad.shiftAssistant.holder

import dev.shiftsad.shiftAssistant.AppConfig
import java.util.concurrent.atomic.AtomicReference

object ConfigHolder {
    private val ref = AtomicReference<AppConfig>()

    fun get(): AppConfig =
        ref.get() ?: error("AppConfig not initialized")

    fun set(config: AppConfig) {
        ref.set(config)
    }

    fun update(transform: (AppConfig) -> AppConfig) {
        ref.updateAndGet { current ->
            current?.let(transform) ?: error("AppConfig not initialized")
        }
    }

    fun isInitialized(): Boolean = ref.get() != null
}