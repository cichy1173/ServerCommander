package com.example.servercommander

import android.app.AlertDialog
import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class SshConnection() {

    private var serverAddress: String = ""
    private var serverPort: Int = 22
    private var username: String = ""
    private var keyPath: String = ""
    private val jsch = JSch()

    constructor(serverAddress: String, serverPort: Int, username: String, keyPath: String): this() {
        this.serverAddress = serverAddress
        this.serverPort = serverPort
        this.username = username
        this.keyPath = keyPath
    }

    fun executeRemoteCommandOneCall(command: String): String {
        if (serverAddress.isNotEmpty() and username.isNotEmpty() and keyPath.isNotEmpty() and command.isNotEmpty())
        {
            if(File("$keyPath/id_rsa").exists() and File("$keyPath/id_rsa.pub").exists())
            {
                val session = jsch.getSession(username, serverAddress, serverPort)
                val properties = Properties()
                properties["StrictHostKeyChecking"] = "no"
                session.setConfig(properties)

                session.connect()

                val sshChannel = session.openChannel("exec") as ChannelExec
                val outputStream = ByteArrayOutputStream()
                sshChannel.outputStream = outputStream

                // Execute command.
                sshChannel.setCommand(command)
                sshChannel.connect()

                // Sleep needed in order to wait long enough to get result back.
                Thread.sleep(1_000)
                sshChannel.disconnect()

                session.disconnect()

                return outputStream.toString()

            }
            return ""
        }
        return ""
    }

    fun generateKeyPair (context: Context): Boolean {
        var ret = false
        val privateKeyFile: String = "id_rsa"
        val publicKeyFile: String = "id_rsa.pub"

        if(Environment.MEDIA_MOUNTED == Environment.getExternalStorageState())
        {
            val keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA)

            val idRsa = File(context.getExternalFilesDir(null), "id_rsa.txt")
            val idRsaPub = File(context.getExternalFilesDir(null), "id_rsa_pub.txt")

            if(idRsa.exists() or idRsaPub.exists()) {
                val builder: AlertDialog.Builder? = context?.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setPositiveButton(context.getString(R.string.overwriteButtonLabel)
                        ) { dialog, id ->
                            keyPair.writePrivateKey(idRsa.absolutePath)
                            keyPair.writePublicKey(idRsaPub.absolutePath, context.getString(R.string.pubkeyComment))
                            keyPair.dispose()

                            Toast.makeText(
                                context,
                                context.getString(R.string.newRsaKeysGenerated),
                                Toast.LENGTH_SHORT
                            ).show()

                            ret = true
                        }
                        setNegativeButton(context.getString(R.string.cancelButtonLabel)
                        ) { dialog, id ->
                            Toast.makeText(context, context.getString(R.string.newRsaKeysNotGenerated), Toast.LENGTH_SHORT).show()
                        }
                        setTitle(context.getString(R.string.cautionLabel))
                        setMessage(context.getString(R.string.overwriteMessage))
                    }
                }
                builder?.create()?.show()
            } else {

                keyPair.writePrivateKey(idRsa.absolutePath)
                keyPair.writePublicKey(idRsaPub.absolutePath, context.getString(R.string.pubkeyComment))
                keyPair.dispose()

                Toast.makeText(context, context.getString(R.string.newRsaKeysGenerated), Toast.LENGTH_SHORT).show()
                ret = true
            }

        } else {
            Toast.makeText(context, context.getString(R.string.externalStorageWriteError), Toast.LENGTH_SHORT).show()
        }

        return ret
    }
    
}