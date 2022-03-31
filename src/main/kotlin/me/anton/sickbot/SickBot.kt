package me.anton.sickbot

import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.runBlocking
import me.anton.sickbot.modules.ModuleHandler
import org.bson.Document

class SickBot {

    companion object{
        lateinit var instance: SickBot
    }

    var kord: Kord = Startup.instance.bot
    var rankDocuments = Startup.instance.rankDocuments
    var configDoc: Document = Startup.instance.document
    private val moduleHandler: ModuleHandler = ModuleHandler()

    init {
        instance = this
    }

    suspend fun setupBot(){
        moduleHandler.register()
        kord.on<ReadyEvent> {
            println("Bot is ready")
            kord.editPresence {
                status = PresenceStatus.Online
                playing("on play.sickmc.net")
            }

            handleConsole()
        }

        kord.login {
            @OptIn(PrivilegedIntent::class)
            intents = Intents.all + Intent.GuildPresences
        }
    }

    private suspend fun stopBot(){
        runBlocking {
            kord.editPresence {
                status = PresenceStatus.Offline
            }
            kord.logout()
        }
    }

    private suspend fun handleConsole(){
        while (true){
            if (readln() == "stop")stopBot()
        }
    }

    fun getConfigValue(key: String): String{
        return configDoc.getString(key)
    }

    fun getIdOfRankGroup(name: String): Snowflake{
        val rankDoc = rankDocuments[1].get("groups", Document::class.java).get(name, Document::class.java)

        return Snowflake(rankDoc.getString("discordRoleID"))
    }

    suspend fun getMainGuild(): Guild{
        return kord.getGuild(Snowflake(getConfigValue("mainGuildID")), EntitySupplyStrategy.rest)!!
    }

    suspend fun getSecondGuild(): Guild{
        return kord.getGuild(Snowflake(getConfigValue("secondGuildID")), EntitySupplyStrategy.rest)!!
    }

}