package dev.shiftsad.shiftAssistant

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Path

object YamlConfigLoader {
    private val mapper: ObjectMapper =
        ObjectMapper(YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .registerModule(KotlinModule.Builder().build())

    fun load(path: Path): AppConfig = mapper.readValue(path.toFile())
}