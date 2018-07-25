package de.zalando.zally.util

import de.zalando.zally.getContextFromOpenAPILiteral
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

class OpenApiUtilTest {

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
        val allRefs = context.unrecordedApi.getAllSchemas()

        val bySchema = allRefs.groupBy { it.schema }
        assertThat(bySchema).hasSize(4)

        context.unrecordedApi.components.schemas["Foo"]?.properties?.get("sibling_bar")?.`$ref`

        val siblingFoo = allRefs.first { it.name == "sibling_foo" }
        assertThat(siblingFoo.schema.`$ref`).isBlank()

        val siblingBar = allRefs.first { it.name == "sibling_bar" }
        assertThat(siblingBar.schema.`$ref`).isEqualTo("#/components/schemas/Bar")

    }
}
