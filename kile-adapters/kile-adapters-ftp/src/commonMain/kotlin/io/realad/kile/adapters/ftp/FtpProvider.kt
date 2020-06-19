package io.realad.kile.adapters.ftp

import io.realad.kile.common.error.FilesystemError
import io.realad.kile.fp.Either

interface FtpProvider {

    fun getConnection(options: FtpOptions): Either<FilesystemError, FtpConnection>

}
