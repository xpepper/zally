package de.zalando.zally.rule.zalando

import de.zalando.zally.getContextFromOpenAPILiteral
import de.zalando.zally.getContextFromSwaggerLiteral
import de.zalando.zally.util.getAllSchemas
import de.zalando.zally.rule.assertThat
import de.zalando.zally.util.OpenAPIPostProcessor
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test

class ExtensibleEnumRule2Test {

    private val rule = ExtensibleEnumRule2()
    private val expectedDescription = "Schema is not an extensible enum"

    @Test
    fun `an empty OpenAPI specification causes no violation`() {
        @Language("YAML")
        val context = getContextFromOpenAPILiteral("""
            openapi: 3.0.0
            info:
              title: Lorem Ipsum
              version: 1.0.0
            paths: {}
        """.trimIndent())
        val violations = rule.validate(context)
        assertThat(violations).isEmpty()
    }

    @Test
    fun `an empty Swagger specification causes no violation`() {
        @Language("YAML")
        val context = getContextFromSwaggerLiteral("""
            swagger: 2.0
            info:
              title: Lorem Ipsum
              version: 1.0.0
            paths: {}
        """.trimIndent())
        val violations = rule.validate(context)
        assertThat(violations).isEmpty()
    }

    @Test
    fun `a parameter with a normal enum causes a violation (Swagger)`() {
        @Language("YAML")
        val context = getContextFromSwaggerLiteral("""
            swagger: 2.0
            info:
              title: Lorem Ipsum
              version: 1.0.0
            parameters:
              Foo:
                name: foo
                in: query
                type: string
                enum:
                  - foo
                  - bar
            paths: {}
        """.trimIndent())
        val violations = rule.validate(context)
        assertThat(violations)
            .pointersEqualTo("/parameters/Foo")
            .descriptionsAllEqualTo(expectedDescription)
    }

    @Test
    fun `a parameter with a normal enum causes a violation (OpenAPI)`() {
        @Language("YAML")
        val context = getContextFromOpenAPILiteral("""
            openapi: 3.0.0
            info:
              title: Lorem Ipsum
              version: 1.0.0
            components:
              parameters:
                Foo:
                  name: foo
                  in: query
                  schema:
                      type: string
                      enum:
                        - foo
                        - bar
            paths: {}
        """.trimIndent())
        val violations = rule.validate(context)
        assertThat(violations)
            .pointersEqualTo("/components/parameters/Foo/schema")
            .descriptionsAllEqualTo(expectedDescription)
    }

//    @Test
//    fun returnsViolationIfAnEnumInModelProperty() {
//        val swagger = getFixture("enum_in_model_property.yaml")
//        val expectedViolation = Violation(
//                description = "Properties/Parameters [status] are not extensible enums",
//                paths = listOf("/definitions/CrawledAPIDefinition/properties/status"))
//
//        val violation = rule.validate(swagger)
//
//        assertThat(violation).isNotNull()
//        assertThat(violation).isEqualTo(expectedViolation)
//    }
//
//    @Test
//    fun returnsViolationIfAnEnumInRequestParameter() {
//        val swagger = getFixture("enum_in_request_parameter.yaml")
//        val expectedViolation = Violation(
//                description = "Properties/Parameters [lifecycle_state, environment] are not extensible enums",
//                paths = listOf("/paths/apis/{api_id}/versions/GET/parameters/lifecycle_state",
//                        "/paths/apis/GET/parameters/environment"))
//
//        val violation = rule.validate(swagger)
//
//        assertThat(violation).isNotNull()
//        assertThat(violation).isEqualTo(expectedViolation)
//    }
//
//    @Test
//    fun returnsNoViolationIfNoEnums() {
//        val swagger = getFixture("no_must_violations.yaml")
//
//        assertThat(rule.validate(swagger)).isNull()
//    }
}
