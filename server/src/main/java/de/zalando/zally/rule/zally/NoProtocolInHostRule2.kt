package de.zalando.zally.rule.zally

import de.zalando.zally.rule.Context
import de.zalando.zally.rule.api.Check
import de.zalando.zally.rule.api.Rule
import de.zalando.zally.rule.api.Severity
import de.zalando.zally.rule.api.Violation

@Rule(
    ruleSet = ZallyRuleSet::class,
    id = "M008",
    severity = Severity.MUST,
    title = "Host should not contain protocol"
)
class NoProtocolInHostRule2 {

    private val desc = "Information about protocol should be placed in schema. Current host value '%s' violates this rule"

    @Check(severity = Severity.MUST)
    fun validate(context: Context): List<Violation> =
        context.api.servers
            .mapNotNull {
                if ("://" in it.url) context.violation(desc.format(it.url), it)
                else null
            }

}
