package com.example.societyhive_test5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Applies the user's chosen M3 colour scheme before setContentView().
 *
 * Call ThemeHelper.apply(this) as the very first line of every Activity's
 * onCreate(), before super.onCreate(), so the theme is set before the
 * window is created.
 *
 * Theme keys (stored in SharedPreferences under "pref_theme_key"):
 *   "crimson"  — default, deep red
 *   "midnight" — indigo blue
 *   "forest"   — forest green
 *   "slate"    — M3 baseline purple
 *   "charcoal" — blue-grey
 */
public final class ThemeHelper {

    static final String PREFS     = "societyhive_prefs";
    static final String KEY_THEME = "pref_theme_key";

    private ThemeHelper() {}

    /** Reads the saved theme key from SharedPreferences and calls setTheme(). */
    public static void apply(Activity activity) {
        String key = activity
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, "crimson");
        activity.setTheme(themeResId(key));
    }

    /** Persists the chosen key to SharedPreferences. */
    public static void save(Context context, String themeKey) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit()
               .putString(KEY_THEME, themeKey)
               .apply();
    }

    /** Returns the current saved key. */
    public static String current(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                      .getString(KEY_THEME, "crimson");
    }

    static int themeResId(String key) {
        switch (key) {
            case "ocean":  return R.style.Theme_SocietyHive_Ocean;
            case "violet": return R.style.Theme_SocietyHive_Violet;
            default:       return R.style.Theme_SocietyHive_Crimson;
        }
    }
}
