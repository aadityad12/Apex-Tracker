package com.example.apextracker

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

/** Production requests are attested by Play Integrity before reaching Firebase AI Logic. */
fun Context.configureAppCheck() {
    Firebase.initialize(context = this)
    Firebase.appCheck.installAppCheckProviderFactory(
        PlayIntegrityAppCheckProviderFactory.getInstance()
    )
}
