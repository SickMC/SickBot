package net.sickmc.sickbot

import com.mongodb.client.model.Filters
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import net.sickmc.sickbot.modules.ModuleHandler
import net.sickmc.sickbot.utils.RoleIDs
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.rankGroupColl
import net.sickmc.sickbot.utils.ranksColl

lateinit var kord: Kord
lateinit var mainGuild: Guild
lateinit var secondGuild: Guild
class SickBot {

    companion object{
        lateinit var instance: SickBot
    }

    private val moduleHandler: ModuleHandler = ModuleHandler()

    init {
        instance = this
    }

    @OptIn(PrivilegedIntent::class)
    suspend fun setupBot(){
        moduleHandler.register()
        kord.on<ReadyEvent> {
            println("Bot is ready")
            kord.editPresence {
                status = PresenceStatus.Online
                playing("on play.sickmc.net")
            }
            mainGuild = getMainGuild()
            secondGuild = getSecondGuild()
            handleConsole()
            RoleIDs
        }

        kord.login {
            intents = Intents.all
        }
    }

    private fun stopBot(){
        runBlocking {
            kord.editPresence {
                status = PresenceStatus.Offline
            }
            kord.logout()
        }
    }

    private fun handleConsole(){
        while (true){
            if (readln() == "stop")stopBot()
        }
    }

    private suspend fun getMainGuild(): Guild{
        return kord.getGuild(Snowflake(config.getString("mainGuildID")), EntitySupplyStrategy.rest)?: error("Main Guild cannot be loaded")
    }

    private suspend fun getSecondGuild(): Guild{
        return kord.getGuild(Snowflake(config.getString("secondGuildID")), EntitySupplyStrategy.rest)?: error("Second guild cannot be loaded")
    }

}