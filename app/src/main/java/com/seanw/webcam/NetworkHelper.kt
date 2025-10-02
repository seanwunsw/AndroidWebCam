package com.seanw.webcam

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.*
import java.util.*

object NetworkHelper {

    /**
     * Convert byte array to hex string
     *
     * @param bytes toConvert
     * @return hexValue
     */
    fun bytesToHex(bytes: ByteArray): String {
        val sbuf = StringBuilder()
        for (idx in bytes.indices) {
            val intVal = bytes[idx].toInt() and 0xff
            if (intVal < 0x10) sbuf.append("0")
            sbuf.append(Integer.toHexString(intVal).uppercase())
        }
        return sbuf.toString()
    }

    /**
     * Get utf8 byte array.
     *
     * @param str which to be converted
     * @return array of NULL if error was found
     */
    fun getUTF8Bytes(str: String): ByteArray? {
        return try {
            str.toByteArray(Charsets.UTF_8)
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Load UTF8withBOM or any ansi text file.
     *
     * @param filename which to be converted to string
     * @return String value of File
     * @throws java.io.IOException if error occurs
     */
    @Throws(IOException::class)
    suspend fun loadFileAsString(filename: String): String = withContext(Dispatchers.IO) {
        val BUFLEN = 1024
        BufferedInputStream(FileInputStream(filename), BUFLEN).use { inputStream ->
            val baos = ByteArrayOutputStream(BUFLEN)
            val bytes = ByteArray(BUFLEN)
            var isUTF8 = false
            var read: Int
            var count = 0
            while (inputStream.read(bytes).also { read = it } != -1) {
                if (count == 0 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
                    isUTF8 = true
                    baos.write(bytes, 3, read - 3) // drop UTF8 bom marker
                } else {
                    baos.write(bytes, 0, read)
                }
                count += read
            }
            if (isUTF8) String(baos.toByteArray(), Charsets.UTF_8) else String(baos.toByteArray())
        }
    }

    /**
     * Returns MAC address of the given interface name.
     *
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return mac address or empty string
     */
    suspend fun getMACAddress(interfaceName: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (interfaceName != null) {
                    if (!intf.name.equals(interfaceName, ignoreCase = true)) continue
                }
                val mac = intf.hardwareAddress
                if (mac == null) return@withContext ""
                val buf = StringBuilder()
                for (aMac in mac) buf.append(String.format("%02X:", aMac))
                if (buf.isNotEmpty()) buf.deleteCharAt(buf.length - 1)
                return@withContext buf.toString()
            }
        } catch (ignored: Exception) {
            // for now eat exceptions
        }
        ""
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    suspend fun getIPAddress(useIPv4: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        val isIPv4 = sAddr?.indexOf(':') ?: -1 < 0

                        if (useIPv4) {
                            if (isIPv4)
                                return@withContext sAddr ?: ""
                        } else {
                            if (!isIPv4) {
                                val delim = sAddr?.indexOf('%') ?: -1 // drop ip6 zone suffix
                                return@withContext if (delim < 0) sAddr?.uppercase() ?: "" 
                                else sAddr.substring(0, delim).uppercase()
                            }
                        }
                    }
                }
            }
        } catch (ignored: Exception) {
            // for now eat exceptions
        }
        ""
    }
}
