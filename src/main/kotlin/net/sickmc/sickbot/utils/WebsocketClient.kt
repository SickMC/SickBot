package net.sickmc.sickbot.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*

val webClient = HttpClient(CIO) {
    install(WebSockets)
}

suspend inline fun listenChannel(
    channel: String, crossinline session: suspend DefaultClientWebSocketSession.() -> Unit
) {
    webClient.webSocket(
        method = HttpMethod.Get,
        host = System.getenv("WEBSOCKET_CLIENT_ADDRESS"),
        port = System.getenv("WEBSOCKET_PORT").toInt(),
        path = "/$channel"
    ) {
        send(Frame.Text("jo"))
        session.invoke(this)
        println("Websocket connected!")
    }
}

suspend fun sendChannelMessage(channel: String, message: String) {
    webClient.webSocket(
        method = HttpMethod.Get,
        host = System.getenv("WEBSOCKET_CLIENT_ADDRESS"),
        port = System.getenv("WEBSOCKET_PORT").toInt(),
        path = "/$channel"
    ) {
        send(message)
    }
}

suspend fun sendChannel(channel: String, message: Frame) {
    webClient.webSocket(
        method = HttpMethod.Get,
        host = System.getenv("WEBSOCKET_CLIENT_ADDRESS"),
        port = System.getenv("WEBSOCKET_PORT").toInt(),
        path = "/$channel"
    ) {
        send(message)
    }
}