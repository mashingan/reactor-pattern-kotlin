package reactor.client

import java.net.Socket
import java.net.UnknownHostException
import java.io.*

fun main(args: Array<String>) {
    val ip = "127.0.0.1"
    val port = 3000
    val socket = Socket(ip, port)
    val outstream = PrintWriter(socket.getOutputStream(), true)
    val instream = BufferedReader(InputStreamReader(
            socket.getInputStream()))
    val stdin = BufferedReader(InputStreamReader(System.`in`))
    println("Client connected to host: $ip port $port")
    println("Type \"bye\" to quit")
    println("Tell what your name to the server...")

    var userinput = stdin.readLine() ?:
        throw IOException("Cannot read from console")
    while (true) {
        if (userinput.isEmpty() || userinput == "bye") {
            break
        }
        outstream.println(userinput)
        println("Sending the input: $userinput")
        println("The server says: ${instream.readLine()}")
        userinput = stdin.readLine() ?: ""
    }
    outstream.close()
    instream.close()
    stdin.close()
    socket.close()
}
