package net.sickmc.sickbot

import dev.kord.core.event.interaction.GuildUserCommandInteractionCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.cancel
import kotlin.system.exitProcess

val restartCommandListener = kord.on<GuildUserCommandInteractionCreateEvent> {
    if (interaction.invokedCommandName != "restart bot") return@on
    kord.cancel()
    exitProcess(0)
}