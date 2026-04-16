package com.example.societyhive_test5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public final class ThemeHelper {

    static final String PREFS     = "societyhive_prefs";
    static final String KEY_THEME = "pref_theme_key";

    private ThemeHelper() {}


    public static void apply(Activity activity) {
        String key = activity
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_THEME, "crimson");
        activity.setTheme(themeResId(key));
    }


    public static void save(Context context, String themeKey) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit()
               .putString(KEY_THEME, themeKey)
               .apply();
    }


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
