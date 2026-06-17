# Add project specific ProGuard rules here.
# By default, the noise in this file is keeping it clean.

# Compose rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep models and data classes safe from obfuscation if needed
-keepclassmembers class * {
    *** component1(...);
    *** component2(...);
    *** component3(...);
}
