package net.sickmc.sickbot

import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.flow.toList
import net.sickmc.sickbot.modules.ModuleHandler
import net.sickmc.sickbot.utils.RoleIDs
import net.sickmc.sickbot.utils.config

lateinit var kord: Kord
lateinit var mainGuild: Guild
lateinit var staffGuild: Guild
class SickBot {

    companion object{
        lateinit var instance: SickBot
    }

    init {
        instance = this
    }

    suspend fun setupBot(){
        kord.on<ReadyEvent> {
            println("Bot has started")
            kord.editPresence {
                status = PresenceStatus.Online
                playing("on play.sickmc.net")
            }
            mainGuild = getMainGuild()
            staffGuild = getSecondGuild()
            mainGuild.getApplicationCommands().toList().forEach {
                it.delete()
            }
            RoleIDs
            ModuleHandler.register()
        }

        @OptIn(PrivilegedIntent::class)
        kord.login {
            intents = Intents.all
        }
    }

    private suspend fun getMainGuild(): Guild{
        return kord.getGuild(Snowflake(config.getString("mainGuildID")), EntitySupplyStrategy.rest)?: error("Main Guild cannot be loaded")
    }

    private suspend fun getSecondGuild(): Guild{
        return kord.getGuild(Snowflake(config.getString("secondGuildID")), EntitySupplyStrategy.rest)?: error("Second guild cannot be loaded")
    }

}