/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal object CalculatorExpression {

    private const val MAX_EXPRESSION_LENGTH = 256
    private const val MAX_PLAIN_RESULT_LENGTH = 12
    private const val MAX_DECIMAL_PLACES = 3
    private val mathContext = MathContext.DECIMAL64

    fun extractSuggestion(textBeforeCursor: String): String? {
        val trimmedEnd = textBeforeCursor.trimEnd()
        if (trimmedEnd.isEmpty() || trimmedEnd.last() != '=') return null

        val expressionEnd = trimmedEnd.length - 1
        if (expressionEnd <= 0) return null

        for (start in 0 until expressionEnd) {
            if (!isPossibleExpressionStart(trimmedEnd, start)) continue
            val expression = trimmedEnd.substring(start, expressionEnd).trim()
            if (expression.isEmpty() || expression.length > MAX_EXPRESSION_LENGTH) continue
            return evaluate(expression) ?: continue
        }
        return null
    }

    fun evaluate(expression: String): String? {
        if (expression.length > MAX_EXPRESSION_LENGTH) return null
        return try {
            val value = Parser(expression).parse() ?: return null
            format(value)
        } catch (_: ArithmeticException) {
            null
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun isPossibleExpressionStart(text: String, index: Int): Boolean {
        val current = text[index]
        if (current != '.' && current != '(' && current != '+' && current != '-' &&
            !current.isDigit()
        ) {
            return false
        }
        if (index == 0) return true
        val previous = text[index - 1]
        return previous.isWhitespace() ||
            previous == '=' ||
            (!previous.isLetterOrDigit() && previous != '.' && previous != ')')
    }

    private fun format(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"

        val rounded = value.setScale(MAX_DECIMAL_PLACES, RoundingMode.HALF_UP)
        val plain = rounded.stripTrailingZeros().toPlainString()
        val requiresScientificNotation =
            value.abs() < BigDecimal.ONE.movePointLeft(MAX_DECIMAL_PLACES) ||
                plain.length > MAX_PLAIN_RESULT_LENGTH
        if (!requiresScientificNotation) return plain

        return DecimalFormat(
            "0.###E0",
            DecimalFormatSymbols(Locale.US),
        ).apply {
            roundingMode = RoundingMode.HALF_UP
        }.format(value)
    }

    private class Parser(private val source: String) {
        private var position = 0

        fun parse(): BigDecimal? {
            val value = parseExpression() ?: return null
            skipWhitespace()
            return value.takeIf { position == source.length }
        }

        private fun parseExpression(): BigDecimal? {
            var value = parseTerm() ?: return null
            while (true) {
                skipWhitespace()
                value = when {
                    consume('+') -> value.add(parseTerm() ?: return null, mathContext)
                    consume('-') -> value.subtract(parseTerm() ?: return null, mathContext)
                    else -> return value
                }
            }
        }

        private fun parseTerm(): BigDecimal? {
            var value = parseFactor() ?: return null
            while (true) {
                skipWhitespace()
                value = when {
                    consume('*') -> value.multiply(parseFactor() ?: return null, mathContext)
                    consume('/') -> value.divide(parseFactor() ?: return null, mathContext)
                    else -> return value
                }
            }
        }

        private fun parseFactor(): BigDecimal? {
            skipWhitespace()
            if (consume('+')) return parseFactor()
            if (consume('-')) return parseFactor()?.negate(mathContext)
            if (consume('(')) {
                val value = parseExpression() ?: return null
                skipWhitespace()
                if (!consume(')')) return null
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): BigDecimal? {
            skipWhitespace()
            val start = position
            var digits = 0
            while (position < source.length && source[position].isDigit()) {
                position++
                digits++
            }
            if (position < source.length && source[position] == '.') {
                position++
                while (position < source.length && source[position].isDigit()) {
                    position++
                    digits++
                }
            }
            if (digits == 0) {
                position = start
                return null
            }
            return BigDecimal(source.substring(start, position))
        }

        private fun consume(character: Char): Boolean {
            if (position >= source.length || source[position] != character) return false
            position++
            return true
        }

        private fun skipWhitespace() {
            while (position < source.length && source[position].isWhitespace()) {
                position++
            }
        }
    }
}
