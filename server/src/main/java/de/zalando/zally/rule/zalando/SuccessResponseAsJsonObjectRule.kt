package de.zalando.zally.rule.zalando

import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Context
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation

@Rule(
    ruleSet = ZalandoRuleSet::class,
    id = "110",
    severity = Severity.MUST,
    title = "Response As JSON Object"
)
class SuccessResponseAsJsonObjectRule {

    private val description = "Always return JSON objects as top-level data structures to support extensibility"

    @Check(severity = Severity.MUST)
    fun checkJSONObjectIsUsedAsSuccessResponseType(context: Context): List<Violation> =
        context.api.paths.values
            .flatMap {
                it.readOperations().orEmpty()
                    .flatMap { it.responses.filter { (resCode, _) -> isSuccess(resCode) }.values }
            }
            .flatMap {
                it.content.entries
                    .filter { (mediaType, _) -> mediaType.contains("json") }
            }.map { it.value.schema }
            .filterNot { schema -> schema.type.isNullOrEmpty() || "object" == schema.type }
            .map { schema -> context.violation(description, schema) }

    private fun isSuccess(codeString: String) = codeString.toIntOrNull() in 200..299
}
