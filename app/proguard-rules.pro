# DataStore ve JSON yedekte enum adı olarak saklanan değerler karartılmasın.
-keep class com.makay.launcher.domain.HomeStyle { *; }
-keep class com.makay.launcher.domain.GestureAction { *; }
-keep class com.makay.launcher.domain.IconShape { *; }
-keep class com.makay.launcher.domain.WorkspaceType { *; }
-keep class com.makay.launcher.domain.AppEdition { *; }
-keep class androidx.core.content.FileProvider { *; }
-keep class androidx.tvprovider.media.tv.** { *; }
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.window.**

