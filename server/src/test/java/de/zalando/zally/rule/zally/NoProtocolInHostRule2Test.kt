package de.zalando.zally.rule.zally

import com.fasterxml.jackson.core.JsonPointer
import de.zalando.zally.getContextFromFixture
import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.OpenAPI
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

class NoProtocolInHostRule2Test {

    private val rule = NoProtocolInHostRule2()

    val expectedViolation = Violation("", emptyList())

    @Test
    fun `empty specification causes no violation`() {
        val context = Context(OpenAPI())
        assertThat(rule.validate(context)).isEmpty()
    }

    @Test
    fun `a server without protocol in URL causes no violation`() {
        @Language("YAML")
        val context = Context.createOpenApiContext("""
            openapi: 3.0.0
            servers:
            - url: google.com
        """.trimIndent())!!
        assertThat(rule.validate(context)).isEmpty()
    }

    @Test
    fun `a server with HTTP causes violations`() {
        @Language("YAML")
        val context = Context.createOpenApiContext("""
            openapi: 3.0.0
            servers:
            - url: http://google.com
        """.trimIndent())!!
        assertThat(rule.validate(context)).containsExactly(
            Violation(
                "Information about protocol should be placed in schema. Current host value 'http://google.com' violates this rule",
                JsonPointer.compile("/servers/0"))
        )
    }

    @Test
    fun `a server with HTTPS causes violation`() {
        @Language("YAML")
        val context = Context.createOpenApiContext("""
            openapi: 3.0.0
            servers:
            - url: https://google.com
        """.trimIndent())!!
        assertThat(rule.validate(context)).containsExactly(
            Violation(
                "Information about protocol should be placed in schema. Current host value 'https://google.com' violates this rule",
                JsonPointer.compile("/servers/0"))
        )
    }

    @Test
    fun `the SPP API generates no violation`() {
        val context = getContextFromFixture("api_spp.json")!!
        assertThat(rule.validate(context)).isEmpty()
    }

    @Test
    fun `PROBLEM -- A Swagger API with a host is automatically converted to a URL with protocol`() {
        @Language("YAML")
        val swaggerYaml = """
            swagger: "2.0"
            info:
              title: Test
            host: test.zalan.do
            schemes:
              - https
            paths:
              /foo:
                get:
                  responses:
                    200:
                      description: It worked!
        """.trimIndent()
        val context = Context.createSwaggerContext(swaggerYaml, true)!!
        assertThat(context.api.servers).noneMatch { it.url.startsWith("https://") }
        assertThat(rule.validate(context)).isEmpty()
    }

    @Test
    fun `the SPA API causes no violation`() {
        val context = getContextFromFixture("api_spa.yaml")!!
        assertThat(rule.validate(context)).isEmpty()
    }

}
