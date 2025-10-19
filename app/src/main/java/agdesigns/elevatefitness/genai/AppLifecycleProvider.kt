package agdesigns.elevatefitness.genai

interface AppLifecycleProvider {
    var isAppInForeground: Boolean
}

class LifecycleProvider : AppLifecycleProvider {
    private var _isAppInForeground = false

    override var isAppInForeground: Boolean
        get() = _isAppInForeground
        set(value) {
            _isAppInForeground = value
        }
}