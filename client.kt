package reactor.client

import java.net.Socket
import java.net.UnknownHostException
//import java.io.*
import java.io.OutputStreamWriter
import java.io.IOException

fun prompt(): String {
    print("> ")
    return readLine() ?: ""
}

fun OutputStreamWriter.println(msg: String) {
    write(msg, 0, msg.length)
    flush()
}

fun main(args: Array<String>) {
    val ip = "127.0.0.1"
    val port = 3000
    val socket = Socket(ip, port)
    val outstream = socket.getOutputStream().writer()
    val instream = socket.getInputStream().bufferedReader()
    println("Client connected to host: $ip port $port")
    println("Type \"bye\" to quit")
    println("Tell what your name to the server...")

    print("> ")
    var userinput = readLine() ?:
        throw IOException("Cannot read from console")
    while (true) {
        if (userinput.isEmpty() || userinput == "bye") {
            break
        }
        outstream.println(userinput)
        println("Sending the input: $userinput")
        println("The server says: ${instream.readLine()}")
        userinput = prompt()
    }
    outstream.close()
    instream.close()
    socket.close()
}
