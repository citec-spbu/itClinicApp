package com.spbu.projecttrack.core.search

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchUtilsTest {

    @Test
    fun doesNotMatchUnrelatedTextByAccidentalBigrams() {
        assertFalse(
            matchesSearchQuery(
                "прив",
                "Сверхдлинный экспериментальный проект по созданию распределенной платформы",
            )
        )
    }

    @Test
    fun matchesWordPrefixInsideLongWord() {
        assertTrue(
            matchesSearchQuery(
                "мед",
                "межведомственной платформы координации медицинских инициатив",
            )
        )
    }

    @Test
    fun toleratesSingleTypoInLongWord() {
        assertFalse(
            matchesSearchQuery(
                "превет",
                "привет",
            )
        )
    }

    @Test
    fun matchesAcrossSeveralFields() {
        assertTrue(
            matchesSearchQuery(
                "ivan backend",
                "Ivan Petrov",
                "Backend team",
            )
        )
    }

    @Test
    fun doesNotMatchUnrelatedProjectNameAndDescription() {
        assertFalse(
            matchesSearchQuery(
                "привет",
                "Citec проект тестового токена",
                "Проект для проверки авторизации и тестового токена",
            )
        )
    }
}
