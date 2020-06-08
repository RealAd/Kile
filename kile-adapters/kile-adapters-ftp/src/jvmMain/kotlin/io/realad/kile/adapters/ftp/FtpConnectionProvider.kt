package io.realad.kile.adapters.ftp

import com.github.aakira.napier.Napier
import io.realad.kile.adapters.ftp.error.AuthenticationFailed
import io.realad.kile.adapters.ftp.error.ConnectToFtpHostFailed
import io.realad.kile.error.FilesystemError
import io.realad.kile.fp.Either
import io.realad.kile.fp.left
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import java.io.IOException

class FtpConnectionProvider : ConnectionProvider<FTPClient> {

    override fun getConnection(options: ConnectionOptions): Either<FilesystemError, FTPClient> {
        return when (val result = createConnection(options)) {
            is Either.Left -> result
            is Either.Right -> authenticate(options, result.r)
        }
    }

    private fun createConnection(options: ConnectionOptions): Either<FilesystemError, FTPClient> {
        val client = FTPClient()
        try {
            client.connect(options.getHost(), options.getPort())
        } catch (e: IOException) {
            Napier.e("Exception in connecting to FTP Server, host: ${options.getHost()}, port: ${options.getPort()}", e)
            return ConnectToFtpHostFailed.forHost(
                options.getHost(),
                options.getPort(),
                FilesystemError(e.localizedMessage)
            ).left()
        }
        if (!FTPReply.isPositiveCompletion(client.replyCode)) {
            Napier.e("FTP reply is not positive, code: ${client.replyCode}, message: ${client.replyString}")
            return ConnectToFtpHostFailed.forHost(options.getHost(), options.getPort()).left()
        }
        return Either.Right(client)
    }

    private fun authenticate(
        options: ConnectionOptions,
        client: FTPClient
    ): Either<FilesystemError, FTPClient> {
        return try {
            if (client.login(options.getUsername(), options.getPassword())) {
                Either.Right(client)
            } else {
                Napier.e("Unable to login/authenticate to FTP server with username: ${options.getUsername()}, code: ${client.replyCode}, message: ${client.replyString}")
                AuthenticationFailed.forLogin(options.getUsername(), client.replyCode, client.replyString).left()
            }
        } catch (e: IOException) {
            Napier.e("Exception in connecting to FTP Server while login with username: ${options.getUsername()}", e)
            ConnectToFtpHostFailed.forHost(options.getHost(), options.getPort(), FilesystemError(e.localizedMessage))
                .left()
        }
    }

}
