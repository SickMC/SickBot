package net.sickmc.sickbot

import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.requestMembers
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.live.LiveMember
import dev.kord.core.live.live
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.RequestGuildMembersBuilder
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.toList
import net.sickmc.sickbot.modules.ModuleHandler
import net.sickmc.sickbot.utils.RoleIDs
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.levelingColl
import net.sickmc.sickbot.utils.toSnowflake
import org.bson.Document

lateinit var kord: Kord
lateinit var mainGuild: Guild
lateinit var staffGuild: Guild
@OptIn(KordPreview::class)
val liveMembers = arrayListOf<LiveMember>()
val databaseMembers = hashMapOf<Member, Document>()
class SickBot {

    companion object{
        lateinit var instance: SickBot
    }

    init {
        instance = this
    }

    @OptIn(PrivilegedIntent::class, KordPreview::class)
    suspend fun setupBot(){
        kord.on<ReadyEvent> {
            kord.editPresence {
                status = PresenceStatus.Online
                playing("on play.sickmc.net")
            }
            mainGuild = getMainGuild()
            staffGuild = getSecondGuild()
            mainGuild.getApplicationCommands().collect {app -> app.delete() }
            val builder: RequestGuildMembersBuilder.() -> Unit = { requestAllMembers() }
            mainGuild.requestMembers(builder).collect { event -> event.members.forEach { liveMembers.add(it.live()) } }
            levelingColl.find().toFlow().collect { databaseMembers[mainGuild.getMember(it.getString("id").toSnowflake())] = it }
            RoleIDs
            ModuleHandler.register()
            println("Bot has started")
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