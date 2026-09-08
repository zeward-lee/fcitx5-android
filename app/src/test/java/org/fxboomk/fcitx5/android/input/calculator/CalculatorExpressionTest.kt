/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fxboomk.fcitx5.android.input.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorExpressionTest {

    @Test
    fun `honors arithmetic precedence`() {
        assertEquals("31", CalculatorExpression.evaluate("12+34-5*6/2"))
    }

    @Test
    fun `supports whitespace decimals and parentheses`() {
        assertEquals(
            "7.5",
            CalculatorExpression.evaluate(" ( 1.5 + 2.5 ) * 2 - .5 "),
        )
    }

    @Test
    fun `supports unary signs`() {
        assertEquals("-6", CalculatorExpression.evaluate("-(2 + 1) * +2"))
    }

    @Test
    fun `formats repeating decimal division with a bounded precision`() {
        assertEquals("0.333", CalculatorExpression.evaluate("1 / 3"))
    }

    @Test
    fun `uses scientific notation for long results`() {
        assertEquals("1.235E12", CalculatorExpression.evaluate("1234567890123"))
        assertEquals("1.235E-6", CalculatorExpression.evaluate("0.0000012345"))
    }

    @Test
    fun `rejects malformed expressions`() {
        assertNull(CalculatorExpression.evaluate("1 + * 2"))
        assertNull(CalculatorExpression.evaluate("(1 + 2"))
        assertNull(CalculatorExpression.evaluate("1 / 0"))
        assertNull(CalculatorExpression.evaluate("1..2"))
    }

    @Test
    fun `extracts the longest valid expression ending at equals`() {
        assertEquals(
            "31",
            CalculatorExpression.extractSuggestion("total: 12 + 34 - 5 * 6 / 2="),
        )
    }

    @Test
    fun `accepts whitespace after equals`() {
        assertEquals("5", CalculatorExpression.extractSuggestion("1.25 * (2 + 2) = "))
    }

    @Test
    fun `extracts an expression after a long text prefix`() {
        val prefix = "x".repeat(400)
        assertEquals(
            "6",
            CalculatorExpression.extractSuggestion("$prefix 1.5 * (2 + 2) ="),
        )
    }

    @Test
    fun `requires equals at the end of the input`() {
        assertNull(CalculatorExpression.extractSuggestion("1 + 2"))
        assertNull(CalculatorExpression.extractSuggestion("1 + 2= text"))
    }
}
