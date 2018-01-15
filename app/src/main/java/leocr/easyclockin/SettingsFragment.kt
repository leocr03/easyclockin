import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import leocr.easyclockin.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle, s: String) {
        addPreferencesFromResource(R.xml.app_preferences)
    }
}