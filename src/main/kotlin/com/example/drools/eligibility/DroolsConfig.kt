package com.example.drools.eligibility

import org.kie.api.KieServices
import org.kie.api.runtime.KieContainer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires Drools into Spring Boot. Creates a singleton KieContainer loaded
 * from classpath rules at startup. Each request creates its own
 * StatelessKieSession from this shared container.
 */
@Configuration
class DroolsConfig {

    companion object {
        private val logger = LoggerFactory.getLogger(DroolsConfig::class.java)
    }

    @Bean
    fun kieContainer(): KieContainer {
        val kieServices = KieServices.Factory.get()
        val kieFileSystem = kieServices.newKieFileSystem()

        // Load all .drl files from classpath rules directory
        val rulesPath = "src/main/resources/rules/cabin-class-eligibility.drl"
        val resource = kieServices.resources
            .newClassPathResource("rules/cabin-class-eligibility.drl")
        kieFileSystem.write(rulesPath, resource)

        val kieBuilder = kieServices.newKieBuilder(kieFileSystem)
        kieBuilder.buildAll()

        logger.info("Drools KieContainer initialised successfully")

        return kieServices.newKieContainer(kieServices.repository.defaultReleaseId)
    }
}
