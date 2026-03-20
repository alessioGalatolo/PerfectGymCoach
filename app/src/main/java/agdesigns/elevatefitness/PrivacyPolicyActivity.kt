package agdesigns.elevatefitness

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = "https://github.com/alessioGalatolo/PerfectGymCoach/blob/main/PRIVACY_POLICY.md"
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        finish()
    }
}