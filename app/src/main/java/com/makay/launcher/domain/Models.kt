package com.makay.launcher.domain

import android.os.UserHandle

enum class HomeStyle {
    GRID,
    NIAGARA,
    TILES,
    DASHBOARD,
    TEXT,
}

enum class FormFactor {
    PHONE,
    TABLET,
    TV,
}

enum class Overlay {
    None,
    Drawer,
    Search,
    ControlCenter,
    Settings,
    WidgetPicker,
    Vault,
    Folder,
    Onboarding,
    Update,
    Idle,
    Calibration,
    ParentalPin,
    TvRows,
}

enum class GestureAction {
    None,
    Drawer,
    Search,
    ControlCenter,
    Notifications,
    Settings,
}

enum class AppCategory(val title: String) {
    Frequent("Sık kullanılan"),
    Social("Sosyal"),
    Communication("İletişim"),
    Media("Medya"),
    Games("Oyun"),
    Productivity("Üretkenlik"),
    Tools("Araçlar"),
    System("Sistem"),
    Other("Diğer"),
}

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val user: UserHandle,
    val category: AppCategory,
    val isSystem: Boolean,
    val usageCount: Int = 0,
    val work: Boolean = false,
    val privateSpace: Boolean = false,
) {
    val key: String get() = if (work || privateSpace) {
        "$packageName/$activityName#${user.hashCode()}"
    } else {
        "$packageName/$activityName"
    }
}

data class AppShortcut(
    val id: String,
    val label: String,
    val packageName: String,
)

data class SearchResult(
    val title: String,
    val subtitle: String,
    val kind: Kind,
    val app: AppInfo? = null,
    val action: (() -> Unit)? = null,
) {
        enum class Kind { App, Contact, Action, Web, Shortcut }
}

data class DeviceStats(
    val batteryPercent: Int = 0,
    val charging: Boolean = false,
    val usedStorageGb: Float = 0f,
    val totalStorageGb: Float = 0f,
    val availMemGb: Float = 0f,
    val totalMemGb: Float = 0f,
)

data class LauncherSettings(
    val homeStyle: HomeStyle = HomeStyle.GRID,
    val swipeUp: GestureAction = GestureAction.Drawer,
    val swipeDown: GestureAction = GestureAction.Search,
    val doubleTap: GestureAction = GestureAction.ControlCenter,
    val showAtAGlance: Boolean = true,
    val showDock: Boolean = true,
    val showAlphabetRail: Boolean = true,
    val textOnly: Boolean = false,
    val gridColumns: Int = 4,
    val searchContacts: Boolean = false,
    val autoCategories: Boolean = true,
    val digitalDetox: Boolean = false,
    val iconPackPackage: String = "",
    val dynamicColors: Boolean = true,
    val onboardingDone: Boolean = false,
    val iconShape: IconShape = IconShape.SQUIRCLE,
    val gridRows: Int = 5,
    val edition: AppEdition = AppEdition.UNSET,
    val updateUrl: String = "",
    val postponedVersionCode: Int = 0,
    val postponedUntil: Long = 0L,
    // TV — kart görünümü
    val tvCardSize: TvCardSize = TvCardSize.MEDIUM,
    val tvCardCorner: Int = 18,
    val tvCardOpacity: Float = 0.85f,
    val tvFocusEffect: TvFocusEffect = TvFocusEffect.SCALE,
    val tvHideRowsAboveFocus: Boolean = false,
    val tvChannelsEnabled: Boolean = true,
    // TV — wallpaper
    val tvWallpaperKind: TvWallpaperKind = TvWallpaperKind.NONE,
    val tvWallpaperUri: String = "",
    val tvWallpaperBrightness: Float = 1f,
    val tvWallpaperBlur: Int = 0,
    val tvWallpaperDim: Float = 0.35f,
    // TV — boot / idle
    val tvAutostartKey: String = "",
    val tvIdleAction: TvIdleAction = TvIdleAction.NONE,
    val tvIdleMinutes: Int = 5,
    // TV — ebeveyn
    val tvParentalEnabled: Boolean = false,
    val tvParentalPinHash: String = "",
    val tvParentalSchedule: Boolean = false,
    val tvParentalStartMin: Int = 21 * 60,
    val tvParentalEndMin: Int = 7 * 60,
    val tvBlockedPackages: String = "",
    val tvBlockedInputs: String = "",
)

enum class IconShape {
    CIRCLE,
    SQUIRCLE,
    TEARDROP,
    ROUNDED,
}

enum class WorkspaceType {
    APP,
    FOLDER,
    WIDGET,
}

data class WorkspaceItem(
    val id: String,
    val type: WorkspaceType,
    val page: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    val appKey: String = "",
    val title: String = "",
    val members: List<String> = emptyList(),
    val widgetId: Int = -1,
    val provider: String = "",
    val color: Int = 0,
    val shortcutId: String = "",
)

data class PinnedWidget(
    val appWidgetId: Int,
    val provider: String,
)

data class WidgetOffer(
    val providerFlattened: String,
    val label: String,
    val minWidth: Int,
    val minHeight: Int,
)

data class IconPackInfo(
    val packageName: String,
    val label: String,
)

data class InputSource(
    val id: String,
    val label: String,
)

data class GlanceChip(
    val label: String,
    val app: AppInfo? = null,
)

data class GlanceSnapshot(
    val greeting: String = "",
    val headline: String = "",
    val detail: String = "",
    val chips: List<GlanceChip> = emptyList(),
    val headphonesConnected: Boolean = false,
    val workHours: Boolean = false,
    val weather: String = "",
)

data class HomeUiState(
    val apps: List<AppInfo> = emptyList(),
    val favorites: List<AppInfo> = emptyList(),
    val dock: List<AppInfo> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val locked: Set<String> = emptySet(),
    val vaultApps: List<AppInfo> = emptyList(),
    val vaultUnlocked: Boolean = false,
    val badges: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val overlay: Overlay = Overlay.None,
    val settings: LauncherSettings = LauncherSettings(),
    val deviceStats: DeviceStats = DeviceStats(),
    val isDefaultLauncher: Boolean = true,
    val shutterApp: AppInfo? = null,
    val shortcuts: List<AppShortcut> = emptyList(),
    val isWideScreen: Boolean = false,
    val flashlightOn: Boolean = false,
    val formFactor: FormFactor = FormFactor.PHONE,
    val widgets: List<PinnedWidget> = emptyList(),
    val widgetOffers: List<WidgetOffer> = emptyList(),
    val iconPacks: List<IconPackInfo> = emptyList(),
    val inputSources: List<InputSource> = emptyList(),
    val glance: GlanceSnapshot = GlanceSnapshot(),
    val gestureNavigation: Boolean = false,
    val wallpaperGeneration: Int = 0,
    val workspace: List<WorkspaceItem> = emptyList(),
    val openFolderId: String? = null,
    val selectedItemId: String? = null,
    val profile: EditionProfile = EditionProfile.of(AppEdition.UNSET),
    val pendingUpdate: UpdateOffer? = null,
    val updateStatus: String = "",
    val tvRows: List<TvRowDef> = emptyList(),
    val tvResolvedRows: List<TvResolvedRow> = emptyList(),
    val tvFocusedRowId: String? = null,
    val tvLastUsed: Map<String, Long> = emptyMap(),
    val parentalUnlockUntil: Long = 0L,
    val pendingParentalApp: AppInfo? = null,
    val pendingParentalInput: InputSource? = null,
    val calibrationPattern: TvCalibrationPattern = TvCalibrationPattern.COLOR_BARS,
    val engineeringMenus: List<String> = emptyList(),
)

data class UpdateOffer(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String = "",
    val notes: String = "",
    val sha256: String = "",
)
