package com.helpteach.offline

import android.app.Application
import com.helpteach.offline.data.AppDatabase

class HelpTeachApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
