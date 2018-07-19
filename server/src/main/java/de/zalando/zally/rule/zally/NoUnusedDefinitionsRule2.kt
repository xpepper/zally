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
import org.slf4j.LoggerFactory

@Rule(
    ruleSet = ZallyRuleSet::class,
    id = "S005",
    severity = Severity.SHOULD,
    title = "Do not leave unused definitions"
)
class NoUnusedDefinitionsRule2 {

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> {
        this.context = context
        return findUnreferencedParameters(context) + findUnreferencedSchemas(context)
    }

    private var context: Context? = null // TODO: KILL MEEEEEEEEE (after debugging)

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
        val allRefs: Set<Schema<*>> = findAllSchemasInApi(context.unrecordedApi).toSet()
        return context.validateSchemas { (name, schema) ->
            log.debug("Finding if schema `$name` is referenced.")
            if (schema in allRefs) {
                emptyList()
            } else {
                log.debug("Schema `$name` is not referenced. Generating violation.")
                context.violations("Unused schema definition.", schema)
            }
        }
    }

    private fun findAllSchemasInApi(api: OpenAPI): List<Schema<*>> {
        val refsInPaths = api.paths.orEmpty().values.flatMap(this::findAllSchemasInPath)
        val refsInSchemaDefinitions = api.components?.schemas.orEmpty().values
            .flatMap { findAllSchemasInSchema(it, includeSelf = false) }
        return refsInPaths + refsInSchemaDefinitions
    }

    private fun findAllSchemasInPath(path: PathItem): List<Schema<*>> {
        return path.readOperations().flatMap(this::findAllSchemasInOperation)
    }

    private fun findAllSchemasInOperation(operation: Operation): List<Schema<*>> {
        val inParameters = operation.requestBody?.content.orEmpty().values
            .flatMap { findAllSchemasInSchema(it.schema) }
        val inResponse = operation.responses.orEmpty().values
            .flatMap { findAllSchemasInResponse(it) }
        return inParameters + inResponse
    }

    private fun findAllSchemasInSchema(schema: Schema<*>): List<Schema<*>> = findAllSchemasInSchema(schema, true)

    private fun findAllSchemasInSchema(schema: Schema<*>, includeSelf: Boolean): List<Schema<*>> {
        val specific = when (schema) {
            is ArraySchema -> {
                val inItemsSchema = findAllSchemasInSchema(schema.items)
                inItemsSchema
            }
            is ComposedSchema -> {
                val inComposedSchema = findAllSchemasInComposedSchema(schema)
                inComposedSchema
            }
            else -> {
                val properties = schema.properties.orEmpty().values.flatMap(this::findAllSchemasInSchema)
                val additionalProperties = findAllRefsInAdditionalProperties(schema.properties)
                properties + additionalProperties
            }
        }
        val self = if (includeSelf) listOf(schema) else emptyList()
        return self + specific
    }

    private fun findAllSchemasInComposedSchema(schema: ComposedSchema): List<Schema<*>> {
        val allOf = schema.allOf.orEmpty().flatMap(this::findAllSchemasInSchema)
        val additionalProperties = findAllRefsInAdditionalProperties(schema.additionalProperties)
        return allOf + additionalProperties
    }

    private fun findAllRefsInAdditionalProperties(additionalProperties: Any?): List<Schema<*>> {
        return when (additionalProperties) {
            is Schema<*> ->
                findAllSchemasInSchema(additionalProperties)
            is Map<*, *> ->
                additionalProperties.values
                    .mapNotNull { it as? Schema<*> }
                    .flatMap(this::findAllSchemasInSchema)
            else ->
                emptyList()
        }
    }

    private fun findAllSchemasInResponse(response: ApiResponse): List<Schema<*>> {
        return response.content.orEmpty().values
            .flatMap { findAllSchemasInSchema(it.schema) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
