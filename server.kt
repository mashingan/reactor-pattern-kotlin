package reactor.server

import java.net.*

import java.nio.channels.*
import java.nio.ByteBuffer
import java.io.*
import java.util.concurrent.*

class Reactor(val port: Int, val withThreadPool: Boolean = true): Runnable {
    val sschannel: ServerSocketChannel
    var selector: Selector
    var selectkey0: SelectionKey
    init {
        sschannel = ServerSocketChannel.open()
        sschannel.socket().bind(InetSocketAddress(port))
        sschannel.configureBlocking(false)
        selector = Selector.open()
        selectkey0 = sschannel.register(selector,
                SelectionKey.OP_ACCEPT)
        selectkey0.attach(object: Runnable {
            override fun run() {
                try {
                    var schan = sschannel.accept()
                    if (schan != null) {
                        if (withThreadPool) {
                            HandlerThreadPool(selector, schan)
                        } else {
                            Handler(selector, schan)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        })
    }

    override fun run() {
        println("Server is listening to port: ${sschannel.socket()?.
                getLocalPort()}")
        try {
            while (!Thread.interrupted()) {
                selector.select()
                val selected = selector.selectedKeys()
                val it = selected.iterator()
                while(it.hasNext()) {
                    ((it.next() as SelectionKey).attachment() as Runnable).run()
                }
                selected.clear()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


enum class State {
    reading, sending, processing
}

open class Handler(var selector: Selector?, var schan: SocketChannel?): Runnable
{

    var state = State.reading
    val selectionkey: SelectionKey?
    val input = ByteBuffer.allocate(1024)
    var clientname: String = ""

    init {
        schan?.configureBlocking(false)
        selectionkey = schan?.register(selector, 0)
        selectionkey?.attach(this)
        selectionkey?.interestOps(SelectionKey.OP_READ)
        selector?.wakeup()
    }

    override fun run() {
        if (state == State.reading) {
            read()
        } else if (state == State.sending) {
            send()
        }
    }

    fun send() {
        println("Saying hello to $clientname")
        val output = ByteBuffer.wrap(("Hello " + clientname + "\n").toByteArray())
        schan?.write(output)
        selectionkey?.interestOps(SelectionKey.OP_READ)
        state = State.reading
    }

    open fun read() {
        val readcount = schan?.read(input) ?: 0
        if (readcount == -1) {
            schan?.close()
        } else {
            if (readcount > 0) readprocess(readcount)
            state = State.sending
            selectionkey?.interestOps(SelectionKey.OP_WRITE)
        }
    }

     fun readprocess(readcount: Int) {
        with(StringBuilder()) {
            input.flip()
            println("count: $readcount")
            val substringbytes = input.array()
                    .copyOf(readcount)
                    .map { it.toChar() }
                    .joinToString(
                            separator = "")
                    .trim()
            println("$substringbytes")
            append(substringbytes)
            input.clear()
            clientname = toString().trim()
        }
    }
}

class HandlerThreadPool: Handler {
    val pool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    )
    constructor(sel: Selector?, s: SocketChannel?): super(sel, s)
    {}

    override fun read() {
        val readcount = schan?.read(input) ?: 0
        if (readcount > 0) {
            state = State.processing
            pool.execute(object: Runnable {
                override fun run() {
                    processAndHandoff(readcount)
                }
            })
            selectionkey?.interestOps(SelectionKey.OP_WRITE)
        }
    }

    fun processAndHandoff(readcount: Int) {
        readprocess(readcount)
        state = State.sending
    }

}

fun main(args: Array<String>) {
    val reactor = Reactor(3000)
    Thread(reactor).start()
}
