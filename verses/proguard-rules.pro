# Verses Library Internal ProGuard Rules

# Preserve all public and @PublishedApi internal APIs.
-keep public class com.woniu0936.verses.** { *; }

# Allow obfuscation of truly internal (non-published) members if any.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# Keep ViewBinding requirements.
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
}

# Keep ItemWrapper and SmartViewHolder as they are accessed via inlined DSL code.
-keep class com.woniu0936.verses.model.ItemWrapper { *; }
-keep class com.woniu0936.verses.model.SmartViewHolder { *; }
