package com.spbu.projecttrack.rating.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for student name-matching logic.
 * This is a critical part of the ranking — incorrect name matching
 * leads to duplicate entries or lost data.
 */
class RatingPersonIdentityTest {

    // ─────────────────────────────────────────────
    // displayPersonName — strips role suffix from name
    // ─────────────────────────────────────────────

    @Test
    fun displayName_plainNameIsReturnedAsIs() {
        assertEquals("Иванов Иван", displayPersonName("Иванов Иван"))
    }

    @Test
    fun displayName_removesRoleSuffix() {
        // "Иванов Иван — Backend Developer" → "Иванов Иван"
        assertEquals("Иванов Иван", displayPersonName("Иванов Иван — Backend Developer"))
    }

    @Test
    fun displayName_removesRoleWithDashSeparator() {
        assertEquals("Петров Пётр", displayPersonName("Петров Пётр - Frontend"))
    }

    @Test
    fun displayName_recognisesRussianRoleKeywords() {
        assertEquals("Сидоров Сидор", displayPersonName("Сидоров Сидор — Разработчик"))
        assertEquals("Кузнецов Кузьма", displayPersonName("Кузнецов Кузьма — Дизайнер"))
        assertEquals("Попов Павел", displayPersonName("Попов Павел — Аналитик"))
    }

    @Test
    fun displayName_recognisesEnglishRoleKeywords() {
        assertEquals("Alice Smith", displayPersonName("Alice Smith — iOS Developer"))
        assertEquals("Bob Jones", displayPersonName("Bob Jones — Team Lead"))
        assertEquals("Carol White", displayPersonName("Carol White — QA Engineer"))
    }

    @Test
    fun displayName_keepsNameWhenTailIsNotARole() {
        // "Иванов — Сидоров" — the tail contains no role keyword
        assertEquals("Иванов — Сидоров", displayPersonName("Иванов — Сидоров"))
    }

    @Test
    fun displayName_handlesNull() {
        assertEquals("", displayPersonName(null))
    }

    @Test
    fun displayName_handlesBlank() {
        assertEquals("", displayPersonName("   "))
    }

    // ─────────────────────────────────────────────
    // personNameKey — generates a key for comparison
    // ─────────────────────────────────────────────

    @Test
    fun personNameKey_singleWordProducesKey() {
        val key = personNameKey("Иванов")
        assertEquals("иванов", key)
    }

    @Test
    fun personNameKey_fullNameProducesKeyWithInitials() {
        // "Иванов Иван Петрович" → "иванов|ип"
        val key = personNameKey("Иванов Иван Петрович")
        assertEquals("иванов|ип", key)
    }

    @Test
    fun personNameKey_twoWordNameProducesKeyWithOneInitial() {
        // "Иванов Иван" → "иванов|и"
        val key = personNameKey("Иванов Иван")
        assertEquals("иванов|и", key)
    }

    @Test
    fun personNameKey_stripsRoleBeforeKeyGeneration() {
        // "Иванов Иван — Backend" and "Иванов Иван" must produce the same key
        assertEquals(personNameKey("Иванов Иван"), personNameKey("Иванов Иван — Backend"))
    }

    @Test
    fun personNameKey_emptyStringProducesEmptyKey() {
        assertEquals("", personNameKey(""))
        assertEquals("", personNameKey(null))
    }

    // ─────────────────────────────────────────────
    // personNameMatches — compares two people
    // ─────────────────────────────────────────────

    @Test
    fun personNameMatches_sameNameMatches() {
        assertTrue(personNameMatches("Иванов Иван", "Иванов Иван"))
    }

    @Test
    fun personNameMatches_caseInsensitive() {
        assertTrue(personNameMatches("иванов иван", "ИВАНОВ ИВАН"))
    }

    @Test
    fun personNameMatches_withAndWithoutRole() {
        // One name has a role suffix, the other does not — they must still match
        assertTrue(personNameMatches("Иванов Иван — Backend", "Иванов Иван"))
    }

    @Test
    fun personNameMatches_extraSpacesIgnored() {
        assertTrue(personNameMatches("Иванов  Иван", "Иванов Иван"))
    }

    @Test
    fun personNameMatches_differentPeopleDoNotMatch() {
        assertFalse(personNameMatches("Иванов Иван", "Петров Пётр"))
    }

    @Test
    fun personNameMatches_nullDoesNotMatch() {
        assertFalse(personNameMatches(null, "Иванов Иван"))
        assertFalse(personNameMatches("Иванов Иван", null))
        assertFalse(personNameMatches(null, null))
    }

    @Test
    fun personNameMatches_partialNameDoesNotMatch() {
        // "Иванов" ≠ "Иванов Иван Петрович" — initials differ
        assertFalse(personNameMatches("Иванов", "Иванов Иван Петрович"))
    }

    // ─────────────────────────────────────────────
    // preferDisplayPersonName — picks the best display name
    // ─────────────────────────────────────────────

    @Test
    fun preferDisplayName_prefersMoreCompleteNameOverShorter() {
        // "Иванов Иван Петрович" is more complete than "Иванов И."
        val result = preferDisplayPersonName("Иванов И.", "Иванов Иван Петрович")
        assertEquals("Иванов Иван Петрович", result)
    }

    @Test
    fun preferDisplayName_keepsCurrent_whenCandidateIsNull() {
        assertEquals("Иванов Иван", preferDisplayPersonName("Иванов Иван", null))
    }

    @Test
    fun preferDisplayName_usesCandidateWhenCurrentIsNull() {
        assertEquals("Иванов Иван", preferDisplayPersonName(null, "Иванов Иван"))
    }

    @Test
    fun preferDisplayName_returnsNull_whenBothNull() {
        assertNull(preferDisplayPersonName(null, null))
    }

    @Test
    fun preferDisplayName_stripsRoleFromBothBeforeComparing() {
        // "Иванов Иван — Backend" vs "Иванов Иван" → both reduce to "Иванов Иван"
        val result = preferDisplayPersonName("Иванов Иван — Backend", "Иванов Иван")
        // Both are equivalent in length; either name without the role is acceptable
        assertEquals("Иванов Иван", result)
    }

    // ─────────────────────────────────────────────
    // normalizeComparableText
    // ─────────────────────────────────────────────

    @Test
    fun normalizeText_lowercasesAndTrimsPunctuation() {
        assertEquals("иванов иван", normalizeComparableText("  Иванов Иван  "))
    }

    @Test
    fun normalizeText_collapsesMulitpleSpaces() {
        assertEquals("a b c", normalizeComparableText("a   b   c"))
    }

    @Test
    fun normalizeText_emptyStringReturnsEmpty() {
        assertEquals("", normalizeComparableText(null))
        assertEquals("", normalizeComparableText(""))
    }
}
