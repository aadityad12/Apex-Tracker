package com.example.apextracker

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

/** Debug builds use a console-allowlisted token; the provider and token never ship in release. */
fun Context.configureAppCheck() {
    Firebase.initialize(context = this)
    Firebase.appCheck.installAppCheckProviderFactory(
        DebugAppCheckProviderFactory.getInstance()
    )
}
