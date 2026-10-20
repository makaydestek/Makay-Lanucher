package com.makay.launcher.data

import com.makay.launcher.domain.HomeUiState
import com.makay.launcher.domain.LauncherSettings
import com.makay.launcher.domain.TvBuiltinRow
import com.makay.launcher.domain.TvRowDef
import com.makay.launcher.domain.TvRowSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvFeaturesTest {
    @Test
    fun defaultLayout_hasActionsAndWatchNext() {
        val layout = TvRowEngine.defaultLayout()
        assertTrue(layout.any { it.builtin == TvBuiltinRow.ACTIONS })
        assertTrue(layout.any { it.builtin == TvBuiltinRow.WATCH_NEXT })
        assertEquals(1, layout.count { it.isDefault })
    }

    @Test
    fun move_and_setDefault() {
        val layout = TvRowEngine.defaultLayout()
        val id = layout[2].id
        val moved = TvRowEngine.move(layout, id, -1)
        assertEquals(id, moved[1].id)
        val withDefault = TvRowEngine.setDefault(moved, id)
        assertTrue(withDefault.first { it.id == id }.isDefault)
        assertEquals(1, withDefault.count { it.isDefault })
    }

    @Test
    fun resolve_hidesInvisibleAndEmptyBuiltin() {
        val state = HomeUiState(settings = LauncherSettings(autoCategories = true))
        val layout = listOf(
            TvRowDef(id = "actions", builtin = TvBuiltinRow.ACTIONS),
            TvRowDef(id = "work", builtin = TvBuiltinRow.WORK, visible = true),
            TvRowDef(id = "letters", builtin = TvBuiltinRow.LETTERS),
            TvRowDef(id = "hidden", builtin = TvBuiltinRow.FAVORITES, visible = false),
        )
        val resolved = TvRowEngine.resolve(layout, state)
        assertTrue(resolved.any { it.isActions })
        assertTrue(resolved.any { it.isLetters })
        assertFalse(resolved.any { it.def.id == "work" })
        assertFalse(resolved.any { it.def.id == "hidden" })
    }

    @Test
    fun resolve_hidesRowsAboveFocus() {
        val layout = listOf(
            TvRowDef(id = "a", builtin = TvBuiltinRow.ACTIONS),
            TvRowDef(id = "b", builtin = TvBuiltinRow.LETTERS),
            TvRowDef(id = "c", builtin = TvBuiltinRow.ACTIONS, title = "Alt"),
        )
        val state = HomeUiState()
        val resolved = TvRowEngine.resolve(layout, state, hideAboveFocusedId = "b")
        assertEquals(listOf("b", "c"), resolved.map { it.def.id })
    }

    @Test
    fun parentalGate_hashAndSchedule() {
        val hash = ParentalGate.hashPin("1234")
        assertEquals(64, hash.length)
        val on = LauncherSettings(
            tvParentalEnabled = true,
            tvParentalSchedule = false,
            tvBlockedPackages = "com.blocked",
            tvParentalPinHash = hash,
        )
        assertTrue(ParentalGate.verifyPin(on, "1234"))
        assertFalse(ParentalGate.verifyPin(on, "0000"))
        assertTrue(ParentalGate.isPackageBlocked(on, "com.blocked"))
        assertFalse(ParentalGate.isPackageBlocked(on, "com.ok"))
    }

    @Test
    fun customRow_factory() {
        val row = TvRowEngine.newCustom("Mine", listOf("a/1", "b/2"))
        assertTrue(row.id.startsWith("custom-"))
        assertEquals(TvRowSort.MANUAL, row.sort)
        assertEquals(2, row.appKeys.size)
    }
}
