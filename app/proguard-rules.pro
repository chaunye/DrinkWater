# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
