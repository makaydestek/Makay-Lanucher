package com.makay.launcher.domain

enum class AppEdition {
    UNSET,
    MOBILE,
    TV,
}

data class EditionProfile(
    val edition: AppEdition,
    val gestures: Boolean,
    val dock: Boolean,
    val workspaceGrid: Boolean,
    val folders: Boolean,
    val widgets: Boolean,
    val controlCenter: Boolean,
    val vault: Boolean,
    val biometric: Boolean,
    val notificationBadges: Boolean,
    val weather: Boolean,
    val headphones: Boolean,
    val contactsSearch: Boolean,
    val calendarGlance: Boolean,
    val mobileHomeStyles: Boolean,
    val tvRows: Boolean,
    val tvInputs: Boolean,
    val glanceSmart: Boolean,
    val bingWallpaper: Boolean,
    val iconPacks: Boolean,
    val search: Boolean,
) {
    val isTv: Boolean get() = edition == AppEdition.TV
    val isMobile: Boolean get() = edition == AppEdition.MOBILE
    val isChosen: Boolean get() = edition != AppEdition.UNSET

    companion object {
        fun of(edition: AppEdition): EditionProfile = when (edition) {
            AppEdition.TV -> EditionProfile(
                edition = edition,
                gestures = false,
                dock = false,
                workspaceGrid = false,
                folders = false,
                widgets = false,
                controlCenter = false,
                vault = false,
                biometric = false,
                notificationBadges = false,
                weather = true,
                headphones = false,
                contactsSearch = false,
                calendarGlance = false,
                mobileHomeStyles = false,
                tvRows = true,
                tvInputs = true,
                glanceSmart = true,
                bingWallpaper = true,
                iconPacks = true,
                search = true,
            )
            AppEdition.MOBILE -> EditionProfile(
                edition = edition,
                gestures = true,
                dock = true,
                workspaceGrid = true,
                folders = true,
                widgets = true,
                controlCenter = true,
                vault = true,
                biometric = true,
                notificationBadges = true,
                weather = true,
                headphones = true,
                contactsSearch = true,
                calendarGlance = true,
                mobileHomeStyles = true,
                tvRows = false,
                tvInputs = false,
                glanceSmart = true,
                bingWallpaper = true,
                iconPacks = true,
                search = true,
            )
            AppEdition.UNSET -> EditionProfile(
                edition = edition,
                gestures = false,
                dock = false,
                workspaceGrid = false,
                folders = false,
                widgets = false,
                controlCenter = false,
                vault = false,
                biometric = false,
                notificationBadges = false,
                weather = false,
                headphones = false,
                contactsSearch = false,
                calendarGlance = false,
                mobileHomeStyles = false,
                tvRows = false,
                tvInputs = false,
                glanceSmart = false,
                bingWallpaper = false,
                iconPacks = false,
                search = false,
            )
        }
    }
}

fun LauncherSettings.forEdition(next: AppEdition): LauncherSettings = when (next) {
    AppEdition.TV -> copy(
        edition = next,
        homeStyle = HomeStyle.GRID,
        showDock = false,
        showAlphabetRail = false,
        searchContacts = false,
        showAtAGlance = false,
        textOnly = false,
        digitalDetox = false,
    )
    AppEdition.MOBILE -> copy(
        edition = next,
        showDock = true,
        showAtAGlance = true,
    )
    AppEdition.UNSET -> copy(edition = next)
}
