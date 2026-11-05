package com.ali.agent.ai

import java.util.Locale
import kotlin.math.pow

object MathAndLogicEngine {

    fun tryEvaluate(input: String): String? {
        val lower = input.lowercase(Locale.US).trim()

        // 1. Percentage calculations: e.g. "what is 18% of 2450"
        val percentRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*%\\s*of\\s*(\\d+(?:\\.\\d+)?)")
        percentRegex.find(lower)?.let { m ->
            val pct = m.groupValues[1].toDoubleOrNull()
            val total = m.groupValues[2].toDoubleOrNull()
            if (pct != null && total != null) {
                val res = (pct / 100.0) * total
                return "${formatNum(pct)}% of ${formatNum(total)} comes out to **${formatNum(res)}**."
            }
        }

        // 2. Unit conversions: km <-> miles, kg <-> lbs, c <-> f
        val unitRegex = Regex("(\\d+(?:\\.\\d+)?)\\s*(km|kilometers|miles|mi|kg|kilograms|lbs|pounds|c|celsius|f|fahrenheit)\\s+(?:to|in)\\s+(km|kilometers|miles|mi|kg|kilograms|lbs|pounds|c|celsius|f|fahrenheit)")
        unitRegex.find(lower)?.let { m ->
            val value = m.groupValues[1].toDoubleOrNull() ?: return null
            val from = m.groupValues[2]
            val to = m.groupValues[3]
            return convertUnits(value, from, to)
        }

        // 3. Arithmetic expression evaluation
        val cleaned = lower
            .replace("what is", "")
            .replace("calculate", "")
            .replace("compute", "")
            .replace("solve", "")
            .replace("plus", "+")
            .replace("minus", "-")
            .replace("times", "*")
            .replace("multiplied by", "*")
            .replace("divided by", "/")
            .replace("x", "*")
            .replace("?", "")
            .trim()

        if (cleaned.matches(Regex("^[0-9+\\-*/().^\\s]+$")) && cleaned.any { it in "+-*/^" } && cleaned.any { it.isDigit() }) {
            return try {
                val value = evalExpression(cleaned)
                if (!value.isNaN() && !value.isInfinite()) {
                    "That works out to **${formatNum(value)}**."
                } else null
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private fun convertUnits(v: Double, from: String, to: String): String? {
        return when {
            from.startsWith("km") && to.startsWith("mi") ->
                "${formatNum(v)} km is **${formatNum(v * 0.621371)} miles**."
            from.startsWith("mi") && to.startsWith("km") ->
                "${formatNum(v)} miles is **${formatNum(v * 1.60934)} km**."
            from.startsWith("kg") && (to.startsWith("lb") || to.startsWith("pound")) ->
                "${formatNum(v)} kg is **${formatNum(v * 2.20462)} lbs**."
            (from.startsWith("lb") || from.startsWith("pound")) && to.startsWith("kg") ->
                "${formatNum(v)} lbs is **${formatNum(v * 0.453592)} kg**."
            from.startsWith("c") && to.startsWith("f") ->
                "${formatNum(v)}°C is **${formatNum((v * 9.0 / 5.0) + 32.0)}°F**."
            from.startsWith("f") && to.startsWith("c") ->
                "${formatNum(v)}°F is **${formatNum((v - 32.0) * 5.0 / 9.0)}°C**."
            else -> null
        }
    }

    private fun formatNum(d: Double): String {
        return if (d == d.toLong().toDouble()) {
            d.toLong().toString()
        } else {
            String.format(Locale.US, "%.3f", d).trimEnd('0').trimEnd('.')
        }
    }

    // Recursive descent arithmetic parser
    private fun evalExpression(expr: String): Double {
        return object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw IllegalArgumentException("Unexpected char")
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw IllegalArgumentException("Unexpected token")
                }
                if (eat('^'.code)) x = x.pow(parseFactor())
                return x
            }
        }.parse()
    }
}
