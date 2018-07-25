package de.zalando.zally.rule.zalando

import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation
import de.zalando.zally.util.getAllSchemas
import de.zalando.zally.util.isEnum

@Rule(
    ruleSet = ZalandoRuleSet::class,
    id = "107",
    severity = Severity.SHOULD,
    title = "Prefer Compatible Extensions"
)
class ExtensibleEnumRule2 {

    @Check(severity = Severity.SHOULD)
    fun validate(context: Context): List<Violation> =
        context.unrecordedApi
            .getAllSchemas()
            .map { it.schema }
            .filter { it.isEnum() }
            .map { context.violation("Schema is not an extensible enum", it) }
}
