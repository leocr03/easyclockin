package leocr.easyclockin

import android.os.Bundle
import android.preference.PreferenceFragment
import leocr.easyclockin.R

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.app_preferences)
    }
}