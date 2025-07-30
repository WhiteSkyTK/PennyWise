# --- Keep rules for Firebase data classes ---

# Make sure the package name 'com.tk.pennywise' is correct for all these classes.
# If any class is in a sub-package (e.g., com.tk.pennywise.models.User),
# adjust its fully qualified name in the rules below.

# For User.kt
-keep class com.tk.pennywise.User { <init>(...); *; }
-keepnames class com.tk.pennywise.User { *; }
-keepclassmembers class com.tk.pennywise.User { *; }

# For Badge.kt
-keep class com.tk.pennywise.Badge { <init>(...); *; }
-keepnames class com.tk.pennywise.Badge { *; }

# For Category.kt (already discussed)
-keep class com.tk.pennywise.Category { <init>(...); *; }
-keepnames class com.tk.pennywise.Category { *; }

# For BudgetGoal.kt
-keep class com.tk.pennywise.BudgetGoal { <init>(...); *; }
-keepnames class com.tk.pennywise.BudgetGoal { *; }

# For CategoryLimit.kt
-keep class com.tk.pennywise.CategoryLimit { <init>(...); *; }
-keepnames class com.tk.pennywise.CategoryLimit { *; }

# For CategoryTotal.kt
# (Only if you save/load this directly to/from Firebase.
# If it's only used locally after querying, it might not need rules for Firebase,
# but it's safer to keep it if unsure, especially if it's passed around)
-keep class com.tk.pennywise.CategoryTotal { <init>(...); *; }
-keepnames class com.tk.pennywise.CategoryTotal { *; }

# For ChartData.kt
# (Same logic as CategoryTotal - keep if used directly with Firebase)
-keep class com.tk.pennywise.ChartData { <init>(...); *; }
-keepnames class com.tk.pennywise.ChartData { *; }

# For EarnedBadge.kt
-keep class com.tk.pennywise.EarnedBadge { <init>(...); *; }
-keepnames class com.tk.pennywise.EarnedBadge { *; }

# For Feedback.kt
-keep class com.tk.pennywise.Feedback { <init>(...); *; }
-keepnames class com.tk.pennywise.Feedback { *; }

# For LoginStreak.kt
-keep class com.tk.pennywise.LoginStreak { <init>(...); *; }
-keepnames class com.tk.pennywise.LoginStreak { *; }

# For Transaction.kt
-keep class com.tk.pennywise.Transaction { <init>(...); *; }
-keepnames class com.tk.pennywise.Transaction { *; }


# --- End of Firebase data class rules ---


# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile