package de.zalando.zally.rule.zally

import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse

@Rule(
        ruleSet = ZallyRuleSet::class,
        id = "S005",
        severity = Severity.SHOULD,
        title = "Do not leave unused definitions"
)
class NoUnusedDefinitionsRule2 {

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
            findUnreferencedParameters(context) + findUnreferencedSchemas(context)

    private fun findUnreferencedParameters(context: Context): List<Violation> {
        val paramsInPaths = context.unrecordedApi.paths.orEmpty().flatMap { (_, path) ->
            path.readOperations().flatMap { operation ->
                operation.parameters.orEmpty()
                        .map { it.`$ref` }
                        .filter { !it.isNullOrBlank() }
            }
        }
        return context.validateParameters { (_, parameter) ->
            val pointer = context.pointerForValue(parameter)
            if (pointer.toString() in paramsInPaths) {
                emptyList()
            } else {
                context.violations("Unused parameter definition: $pointer", pointer)
            }
        }
    }

    private fun findUnreferencedSchemas(context: Context): List<Violation> {
        val allRefs: Set<String> = findAllRefs(context.unrecordedApi).toSet()
        return context.validateSchemas { (_, schema) ->
            val pointer = context.pointerForValue(schema)
            if (pointer.toString() in allRefs) {
                emptyList()
            } else {
                context.violations("Unused schema definition: $pointer", pointer)
            }
        }
    }

    private fun findAllRefs(api: OpenAPI): List<String> =
            findAllRefs(api.paths) + findAllRefs(api.components)

    private fun findAllRefs(paths: Paths?): List<String> =
            paths.orEmpty().flatMap { (_, path) ->
                path.readOperations().flatMap { operation ->
                    val inParams = operation.parameters.orEmpty().flatMap(this::findAllRefs)
                    val inResponse = operation.responses.orEmpty().values.flatMap(this::findAllRefs)
                    inParams + inResponse
                }
            }

    private fun findAllRefs(components: Components?): List<String> =
            components?.schemas?.values.orEmpty().flatMap(::findAllRefs)

    private fun findAllRefs(param: Parameter): List<String> =
            listOfNotNull(param.`$ref`.takeIf { !it.isNullOrBlank() }) +
                    findAllRefs(param.schema)

    private fun findAllRefs(schema: Schema<*>?): List<String> =
            if (schema === null) emptyList()
            else listOfNotNull(schema.`$ref`.takeIf { !it.isNullOrBlank() })

    private fun findAllRefs(apiResponse: ApiResponse): List<String> =
            listOfNotNull(apiResponse.`$ref`.takeIf { !it.isNullOrBlank() }) + findAllRefs(apiResponse.content)

    private fun findAllRefs(content: Content?): List<String> = content
            .orEmpty()
            .values
            .map { it.schema }
            .flatMap(::findAllRefs)

}
