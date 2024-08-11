package space.diomentia.ptm_dct

import android.app.Application
import space.diomentia.ptm_dct.data.ApplicationSettings

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationSettings.init(applicationContext)
    }
}