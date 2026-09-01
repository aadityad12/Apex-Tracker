package com.example.apextracker.media

import android.service.notification.NotificationListenerService

/**
 * Deliberately empty. This service exists only so the app has a component the system recognises as
 * a notification listener, which is the *sole* published way for a non-system app to reach
 * `MediaSessionManager.getActiveSessions()` — the alternative, `MEDIA_CONTENT_CONTROL`, is a
 * signature permission no third-party app can hold.
 *
 * It reads nothing. `onNotificationPosted` and `onNotificationRemoved` are not overridden, so no
 * notification ever enters this process: the grant is used exclusively as the key to the media
 * session, which is what the focus surface's transport controls drive. That is the whole answer to
 * "why does a study tracker want notification access", and it is the answer given to the user in
 * the panel's own connect prompt, in docs/privacy-policy.md, and to Play review in
 * docs/play-console-permissions-declaration.md — keep those three and this comment in agreement.
 *
 * If a future change ever does read a notification here, all four of those have to change with it.
 */
class ApexMediaListenerService : NotificationListenerService()
