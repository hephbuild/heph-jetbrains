package io.github.hephbuild.heph.core

import io.github.hephbuild.heph.HephBundle

/** The one place that turns a failed [HephBinaryResolver] result into user-facing text. */
object HephBinaryMessages {
    fun title(failure: HephBinaryResult.Failure): String = when (failure) {
        is HephBinaryResult.NotFound -> HephBundle.message("notification.binary.notFound.title")
        is HephBinaryResult.NotAFile -> HephBundle.message("notification.binary.notAFile.title")
        is HephBinaryResult.NotExecutable -> HephBundle.message("notification.binary.notExecutable.title")
        is HephBinaryResult.Inaccessible -> HephBundle.message("notification.binary.inaccessible.title")
    }

    fun content(failure: HephBinaryResult.Failure): String = when (failure) {
        is HephBinaryResult.NotFound.Configured ->
            HephBundle.message("notification.binary.notFound.configured.content", failure.value)
        is HephBinaryResult.NotFound.AutoDetect ->
            HephBundle.message("notification.binary.notFound.autodetect.content", searched(failure))
        is HephBinaryResult.NotAFile -> HephBundle.message("notification.binary.notAFile.content", failure.value)
        is HephBinaryResult.NotExecutable ->
            HephBundle.message("notification.binary.notExecutable.content", failure.value)
        is HephBinaryResult.Inaccessible ->
            HephBundle.message("notification.binary.inaccessible.content", failure.value)
    }

    /** De-dupe key: the kind of problem plus the configured value, so a different bad value notifies again. */
    fun key(failure: HephBinaryResult.Failure): String = "binary:" + when (failure) {
        is HephBinaryResult.NotFound.Configured -> "notFound.configured:${failure.value}"
        is HephBinaryResult.NotFound.AutoDetect -> "notFound.autodetect"
        is HephBinaryResult.NotAFile -> "notAFile:${failure.value}"
        is HephBinaryResult.NotExecutable -> "notExecutable:${failure.value}"
        is HephBinaryResult.Inaccessible -> "inaccessible:${failure.value}"
    }

    fun searched(failure: HephBinaryResult.NotFound.AutoDetect): String = failure.searched.joinToString(", ")
}
