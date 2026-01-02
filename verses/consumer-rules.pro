# Verses Library ProGuard Rules
# These rules are automatically applied to any app that depends on this library.

# 1. Preserve ViewBinding inflate methods.
# The DSL relies on method references like ItemUserBinding::inflate.
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
}

# 2. Keep Generic Signatures and Metadata.
# Required for reified type parameters and correct DiffUtil behavior.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# 3. Protect internal model integrity.
# ItemWrapper is the unit of diffing; its members must not be stripped or renamed.
-keepclassmembers class com.woniu0936.verses.model.ItemWrapper { *; }

# 4. Preserve RecyclerView ViewHolder requirements.
-keepclassmembers class * extends androidx.recyclerview.widget.RecyclerView$ViewHolder {
    <init>(...);
}
