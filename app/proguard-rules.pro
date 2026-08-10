# R8 rules for release builds (Issue #198).
#
# Minification is on, which means R8 renames classes, fields and methods. Anything whose *name*
# is load-bearing at runtime has to be kept here, and in this app that means one thing above all:
# the backup file format.
#
# `backupGson()` serializes BackupData and every Room entity by reflecting over their field names.
# If R8 renames those fields, a release build writes a backup whose keys are `a`, `b`, `c` — and,
# worse, cannot read a backup written by any build with different renaming. That is silent data
# loss in the one feature whose entire purpose is data safety, and it would not show up in any
# test that runs on a debug build.
#
# After changing anything here, verify the round trip specifically: export a backup from a debug
# build and restore it into a minified release build, and vice versa.

# Preserve the line numbers in stack traces, and map them back via the R8 mapping file.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Gson
# ---------------------------------------------------------------------------
# Signature is required for Gson's generic type resolution (List<BudgetItem> etc.); without it
# every collection in BackupData deserializes as List<LinkedTreeMap> instead of the real type.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*

-dontwarn sun.misc.**
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ---------------------------------------------------------------------------
# The Gson-serialized model: BackupData plus every entity it contains.
# Field names here ARE the backup file's schema. Keep the members, not just the classes.
# ---------------------------------------------------------------------------
-keepclassmembers class com.example.apextracker.BackupData { <fields>; }
-keepclassmembers class com.example.apextracker.BudgetItem { <fields>; }
-keepclassmembers class com.example.apextracker.Category { <fields>; }
-keepclassmembers class com.example.apextracker.Subscription { <fields>; }
-keepclassmembers class com.example.apextracker.StudySession { <fields>; }
-keepclassmembers class com.example.apextracker.ScreenTimeSession { <fields>; }
-keepclassmembers class com.example.apextracker.ExcludedApp { <fields>; }
-keepclassmembers class com.example.apextracker.Reminder { <fields>; }
-keepclassmembers class com.example.apextracker.Note { <fields>; }
-keepclassmembers class com.example.apextracker.Goal { <fields>; }
-keepclassmembers class com.example.apextracker.GoalCompletion { <fields>; }
-keepclassmembers class com.example.apextracker.AppUsageLimit { <fields>; }
-keepclassmembers class com.example.apextracker.Paper { <fields>; }
-keepclassmembers class com.example.apextracker.PaperTopic { <fields>; }

# Recurrence is Gson-serialized twice over: into the backup file, and into a TEXT column by
# Converters (and into Firestore by FirebaseManager). Its field names are persisted data.
-keepclassmembers class com.example.apextracker.Recurrence { <fields>; }

# ---------------------------------------------------------------------------
# Enums serialized by name
# ---------------------------------------------------------------------------
# Gson writes enum constants by name, and ApexTheme round-trips through Firestore via
# valueOf(themeName). Renaming a constant would make every stored value fail to parse.
-keepclassmembers enum com.example.apextracker.RecurrenceFrequency { *; }
-keepclassmembers enum com.example.apextracker.RecurrenceEndType { *; }
-keepclassmembers enum com.example.apextracker.ReminderPriority { *; }
-keepclassmembers enum com.example.apextracker.ui.design.ApexTheme { *; }

# java.time.DayOfWeek reaches Gson through Recurrence.customDays.
-keepclassmembers enum java.time.DayOfWeek { *; }

# The generic enum contract Gson relies on.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Note: GoalType / GoalCadence / GoalMetric / GoalComparator are `object`s of String constants,
# not enums, so their *values* are inlined string literals and survive minification untouched.
# They need no rule here.
