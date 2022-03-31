package me.anton.sickbot

import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Guild
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bson.Document

class SickBot {

    companion object{
        lateinit var instance: SickBot
    }

    var kord: Kord = Startup.instance.bot
    private val scope = CoroutineScope(Dispatchers.Default)
    lateinit var mainGuild: Guild
    lateinit var staffGuild: Guild
    var rankDocuments = Startup.instance.rankDocuments
    var configDoc: Document = Startup.instance.document

    init {
        instance = this

        setupBot()
    }

    private fun setupBot(){
        scope.launch {
            kord.login()
            kord.on<ReadyEvent> {
                kord.editPresence {
                    status = PresenceStatus.Online
                    playing("on play.sickmc.net")

                    mainGuild = kord.getGuild(Snowflake(getConfigValue("mainGuildID")), EntitySupplyStrategy.rest)!!
                    staffGuild = kord.getGuild(Snowflake(getConfigValue("secondGuildID")), EntitySupplyStrategy.rest)!!
                }

                sendPrefix()
            }

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

    private fun sendPrefix(){
        while (true){
            if (readln() == "stop")stopBot()
        }
    }

    fun getConfigValue(key: String): String{
        return configDoc.getString(key)
    }

    fun getIdOfRankGroup(name: String): Snowflake{
        val rankDoc = rankDocuments[2].get("groups", Document::class.java).get(name, Document::class.java)

        return Snowflake(rankDoc.getString("discordRoleID"))
    }

}