package de.zalando.zally.rule.zally

import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
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
                val fromParameters = operation.parameters.orEmpty()
                val fromBody = listOfNotNull(operation.requestBody)
                fromParameters + fromBody
            }
        }
        return context.validateParameters { (_, parameter) ->
            if (parameter in paramsInPaths) {
                emptyList()
            } else {
                context.violations("Unused parameter definition.", parameter)
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
                context.violations("Unused schema definition.", pointer)
            }
        }
    }

    private fun findAllRefs(api: OpenAPI): List<String> {
        val refsInPaths = api.paths.orEmpty().values.flatMap(this::findAllRefs)
        val refsInDefinitions = api.components?.schemas.orEmpty().values.flatMap(this::findAllRefs)
        return refsInPaths + refsInDefinitions
    }

    private fun findAllRefs(path: PathItem): List<String> =
        path.readOperations().flatMap(this::findAllRefs)


    private fun findAllRefs(operation: Operation): List<String> {
        val inParameters = operation.requestBody?.content?.values.orEmpty().map { it.schema }.flatMap(this::findAllRefs)  //operation.parameters.orEmpty().flatMap(this::findAllRefs)
        val inResponse = operation.responses.orEmpty().values.flatMap(this::findAllRefs)
        return inParameters + inResponse
    }

    private fun findAllRefs(schema: Schema<*>): List<String> =
        when (schema) {
            is ArraySchema ->
                findAllRefs(schema.items)
            is ComposedSchema ->
                findAllRefsFromComposedSchema(schema)
            else -> {
                schema.properties.orEmpty().values.flatMap(this::findAllRefs) +
                    findAllRefsFromProperties(schema.properties)
            }
        }

    private fun findAllRefsFromComposedSchema(schema: ComposedSchema): List<String> =
        schema.allOf.orEmpty().flatMap(this::findAllRefs) +
            findAllRefsFromProperties(schema.additionalProperties)

    private fun findAllRefsFromProperties(additionalProperties: Any?): List<String> =
        when(additionalProperties) {
            is Schema<*> ->
                findAllRefs(additionalProperties)
            else ->
                emptyList()
        }

    private fun findAllRefs(response: ApiResponse): List<String> =
        response.content.orEmpty()
            .values
            .mapNotNull { it.schema }
            .flatMap(this::findAllRefs)

}
