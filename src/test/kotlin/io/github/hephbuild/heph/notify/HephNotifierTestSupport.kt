package io.github.hephbuild.heph.notify

import io.github.hephbuild.heph.rpc.HephNotificationDto

/** Removes and returns everything the notifier has queued for the frontend. */
fun HephNotifier.drain(): List<HephNotificationDto> =
    generateSequence { events.tryReceive().getOrNull() }.toList()
