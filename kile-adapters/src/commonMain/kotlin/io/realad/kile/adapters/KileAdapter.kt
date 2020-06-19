package io.realad.kile.adapters

import io.realad.kile.common.error.FilesystemError
import io.realad.kile.fp.Either

/**
 * Base interface for any file system.
 */
interface KileAdapter {

    /**
     * Function for displaying catalogs and content.
     */
    fun listContents(path: String): Either<FilesystemError, List<String>>

    /**
     * Function to check if a file exists.
     */
    fun fileExists(location: String): Either<FilesystemError, Boolean>

}
