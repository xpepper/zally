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
import io.swagger.v3.oas.models.media.ObjectSchema
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
        val allRefs: Set<Schema<*>> = findAllSchemas(context.unrecordedApi).toSet()
        return context.validateSchemas { (_, schema) ->
            if (schema in allRefs) {
                emptyList()
            } else {
                context.violations("Unused schema definition.", schema)
            }
        }
    }

    private fun findAllSchemas(api: OpenAPI): List<Schema<*>> {
        val refsInPaths = api.paths.orEmpty().values.flatMap(this::findAllSchemas)
        val refsInDefinitions = api.components?.schemas.orEmpty().values
            .flatMap { this.findAllSchemas(it, includeSelf = false) }
        return refsInPaths + refsInDefinitions
    }

    private fun findAllSchemas(path: PathItem): List<Schema<*>> =
        path.readOperations().flatMap(this::findAllSchemas)


    private fun findAllSchemas(operation: Operation): List<Schema<*>> {
        val inParameters = operation.requestBody?.content?.values.orEmpty().map { it.schema }.flatMap(this::findAllSchemas)  //operation.parameters.orEmpty().flatMap(this::findAllSchemas)
        val inResponse = operation.responses.orEmpty().values.flatMap(this::findAllSchemas)
        return inParameters + inResponse
    }

    private fun findAllSchemas(schema: Schema<*>): List<Schema<*>> = findAllSchemas(schema, true)

    private fun findAllSchemas(schema: Schema<*>, includeSelf: Boolean): List<Schema<*>> =
        when (schema) {
            is ArraySchema ->
                findAllSchemas(schema.items)
            is ComposedSchema ->
                findAllSchemas(schema)
            is ObjectSchema -> {
                val self = if (includeSelf) listOf(schema) else emptyList()
                val properties = schema.properties.orEmpty().values.flatMap(this::findAllSchemas)
                val additionalProperties = findAllRefsFromProperties(schema.properties)
                self + properties + additionalProperties
            }
            else ->
                emptyList()
        }

    private fun findAllSchemas(schema: ComposedSchema): List<Schema<*>> =
        schema.allOf.orEmpty().flatMap(this::findAllSchemas) +
            findAllRefsFromProperties(schema.additionalProperties)

    private fun findAllRefsFromProperties(additionalProperties: Any?): List<Schema<*>> =
        when (additionalProperties) {
            is Schema<*> ->
                findAllSchemas(additionalProperties)
            else ->
                emptyList()
        }

    private fun findAllSchemas(response: ApiResponse): List<Schema<*>> =
        response.content.orEmpty()
            .values
            .mapNotNull { it.schema }
            .flatMap(this::findAllSchemas)

}
