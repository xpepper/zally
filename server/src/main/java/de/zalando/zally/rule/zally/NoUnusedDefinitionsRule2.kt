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
    fun validate(context: Context): List<Violation> {

        val paramsInPaths = context.api.paths.orEmpty().flatMap { (_, path) ->
            path.readOperations().flatMap { operation ->
                operation.parameters.orEmpty()
            }
        }

        val allRefs = findAllRefs(context.api).toSet()

        TODO()
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
        listOfNotNull(param.`$ref`) + findAllRefs(param.schema)

    private fun findAllRefs(schema: Schema<*>?): List<String> =
        if (schema === null) emptyList()
        else {
            listOfNotNull(schema.`$ref`)
        }

    private fun findAllRefs(apiResponse: ApiResponse): List<String> =
        listOfNotNull(apiResponse.`$ref`) + findAllRefs(apiResponse.content)

    private fun findAllRefs(content: Content?): List<String> = content
        .orEmpty()
        .values
        .map { it.schema }
        .flatMap(::findAllRefs)

}
