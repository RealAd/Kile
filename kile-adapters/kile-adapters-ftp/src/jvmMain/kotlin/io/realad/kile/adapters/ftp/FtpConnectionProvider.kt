package io.realad.kile.adapters.ftp

import com.github.aakira.napier.Napier
import io.realad.kile.adapters.ftp.error.AuthenticationFailed
import io.realad.kile.adapters.ftp.error.ConnectToFtpHostFailed
import io.realad.kile.common.error.FilesystemError
import io.realad.kile.fp.Either
import io.realad.kile.fp.left
import io.realad.kile.fp.right
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

actual class FtpConnectionProvider : FtpProvider {

    override fun getConnection(options: FtpOptions): Either<FilesystemError, FtpConnection> {
        return when (val unauthenticatedConnection = createConnection(options)) {
            is Either.Left -> unauthenticatedConnection
            is Either.Right -> when (val authenticatedConnection = authenticate(options, unauthenticatedConnection.r)) {
                is Either.Left -> authenticatedConnection
                is Either.Right -> ApacheFtpConnection(authenticatedConnection.r).right()
            }
        }
    }

    private fun createConnection(options: FtpOptions): Either<FilesystemError, FTPClient> {
        try {
            val client = FTPClient().apply { connect(options.getHost(), options.getPort()) }
            return if (FTPReply.isPositiveCompletion(client.replyCode)) {
                client.right()
            } else {
                Napier.e("FTP reply is not positive, code: ${client.replyCode}, message: ${client.replyString}")
                ConnectToFtpHostFailed.forHost(options.getHost(), options.getPort()).left()
            }
        } catch (e: IOException) {
            Napier.e("Exception in connecting to FTP Server, host: ${options.getHost()}, port: ${options.getPort()}", e)
            return ConnectToFtpHostFailed.forHost(
                options.getHost(),
                options.getPort(),
                FilesystemError(e.localizedMessage)
            ).left()
        }
    }

    private fun authenticate(
        options: FtpOptions,
        client: FTPClient
    ): Either<FilesystemError, FTPClient> {
        return try {
            if (client.login(options.getUsername(), options.getPassword())) {
                client.right()
            } else {
                Napier.e("Unable to login/authenticate to FTP server with username: ${options.getUsername()}," +
                    " code: ${client.replyCode}, message: ${client.replyString}")
                AuthenticationFailed.forLogin(options.getUsername(), client.replyCode, client.replyString).left()
            }
        } catch (e: IOException) {
            Napier.e("Exception in connecting to FTP Server while login with username: ${options.getUsername()}", e)
            ConnectToFtpHostFailed.forHost(options.getHost(), options.getPort(), FilesystemError(e.localizedMessage))
                .left()
        }
    }

}
