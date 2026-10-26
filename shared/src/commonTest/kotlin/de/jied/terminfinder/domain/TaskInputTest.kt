package de.jied.terminfinder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskInputTest {

    @Test
    fun togglingAddsToTheEndAndRemovesFromAnywhere() {
        assertEquals("122210", TaskInput.toggleLocation("", 122210))
        assertEquals("122210, 122217", TaskInput.toggleLocation("122210", 122217))
        assertEquals("122217", TaskInput.toggleLocation("122210, 122217", 122210))
    }

    @Test
    fun togglingTheLastOfficeOffClearsTheField() {
        assertEquals("", TaskInput.toggleLocation("122210", 122210))
        assertEquals(emptyList(), TaskInput.locationIds(""))
    }

    @Test
    fun theToggledFormatIsTheOneTheFieldsArePrefilledWith() {
        val prefilled = listOf(122210, 122217).joinToString(TaskInput.SEPARATOR)

        assertEquals(prefilled, TaskInput.toggleLocation("122210", 122217))
        assertEquals(listOf(122210, 122217), TaskInput.locationIds(prefilled))
    }

    @Test
    fun digitsCommasAndSpacesAreAnIdList() {
        assertTrue(TaskInput.looksLikeIds("122210"))
        assertTrue(TaskInput.looksLikeIds("122210, 122217"))
        assertTrue(TaskInput.looksLikeIds("122210,"))
    }

    @Test
    fun anythingWithLettersIsAName() {
        assertFalse(TaskInput.looksLikeIds("mitte"))
        assertFalse(TaskInput.looksLikeIds("122210, abc"))
        assertFalse(TaskInput.looksLikeIds(""))
    }

    @Test
    fun aHandEditedListThatDoesNotParseIsReplacedByThePick() {
        assertNull(TaskInput.locationIds("122210, abc"))
        assertEquals("122217", TaskInput.toggleLocation("122210, abc", 122217))
    }
}
