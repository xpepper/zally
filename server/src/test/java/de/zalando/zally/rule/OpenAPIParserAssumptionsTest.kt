package de.zalando.zally.rule

import de.zalando.zally.getContextFromOpenAPILiteral
import de.zalando.zally.util.getAllSchemas
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

class OpenAPIParserAssumptionsTest {

    private val ref = "\$ref"

    @Test
    fun `circular references cause non complete resolving of schemas`() {
        @Language("YAML")
        val context = getContextFromOpenAPILiteral("""

            openapi: 3.0.0
            info:
              version: 0.0.0
              title: title
            paths:
              /foo:
                get:
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        "application/json":
                          schema:
                            $ref: '#/components/schemas/Bar'
            components:
              schemas:
                Foo:
                  type: object
                  properties:
                    foo_name:
                      type: string
                    sibling_bar:
                      $ref: '#/components/schemas/Bar'
                Bar:
                  type: object
                  properties:
                    bar_name:
                      type: string
                    sibling_foo:
                      $ref: '#/components/schemas/Foo'

        """.trimIndent())

        val schemaFoo = context.unrecordedApi.components?.schemas?.get("Foo")
        val schemaBar = context.unrecordedApi.components?.schemas?.get("Bar")

        val fooGetResponse200ContentSchema =
            context.unrecordedApi.paths?.get("/foo")?.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertThat(fooGetResponse200ContentSchema).isNotNull
        assertThat(fooGetResponse200ContentSchema?.`$ref`).isBlank()
        assertThat(fooGetResponse200ContentSchema).isSameAs(schemaBar)

        val siblingBar = context.unrecordedApi.components.schemas["Foo"]?.properties?.get("sibling_bar")
        assertThat(siblingBar).isNotNull
        assertThat(siblingBar?.`$ref`).isEqualTo("#/components/schemas/Bar")

        val siblingFoo = context.unrecordedApi.components.schemas["Bar"]?.properties?.get("sibling_foo")
        assertThat(siblingFoo).isNotNull
        assertThat(siblingFoo?.`$ref`).isBlank()
        assertThat(siblingFoo).isSameAs(schemaFoo)

    }

    @Test
    fun `no circular references and all refs are resolved`() {
        @Language("YAML")
        val context = getContextFromOpenAPILiteral("""

            openapi: 3.0.0
            info:
              version: 0.0.0
              title: title
            paths:
              /foo:
                get:
                  responses:
                    200:
                      description: Lorem Ipsum
                      content:
                        "application/json":
                          schema:
                            $ref: '#/components/schemas/Bar'
            components:
              schemas:
                Foo:
                  type: object
                  properties:
                    foo_name:
                      type: string
                Bar:
                  type: object
                  properties:
                    bar_name:
                      type: string
                    sibling_foo:
                      $ref: '#/components/schemas/Foo'

        """.trimIndent())

        val fooGetResponse200ContentSchema =
            context.unrecordedApi.paths?.get("/foo")?.get?.responses?.get("200")?.content?.get("application/json")?.schema
        assertThat(fooGetResponse200ContentSchema).isNotNull
        assertThat(fooGetResponse200ContentSchema?.`$ref`).isBlank()

        val siblingFoo = context.unrecordedApi.components.schemas["Bar"]?.properties?.get("sibling_foo")
        assertThat(siblingFoo).isNotNull
        assertThat(siblingFoo?.`$ref`).isBlank()

        assertThat(context.unrecordedApi.getAllSchemas().map { it.schema }.filter { !it.`$ref`.isNullOrBlank() })
            .isEmpty()
    }

}
