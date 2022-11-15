package net.sickmc.sickbot

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.requestMembers
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.builder.RequestGuildMembersBuilder
import net.sickmc.sickapi.rank.loadRanks
import net.sickmc.sickbot.modules.leveling.LevelUser
import net.sickmc.sickbot.modules.leveling.levelingCache
import net.sickmc.sickbot.modules.leveling.registerLevelingHandlers
import net.sickmc.sickbot.modules.registerStaffModule
import net.sickmc.sickbot.modules.registerWelcomeHandlers
import net.sickmc.sickbot.modules.ticket.registerTicketHandlers
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.leveling
import org.litote.kmongo.eq

lateinit var kord: Kord
lateinit var mainGuild: Guild
lateinit var staffGuild: Guild

@OptIn(PrivilegedIntent::class)
object SickBot {

    suspend fun setupBot() {
        kord.on<ReadyEvent> {
            loadRanks()
            kord.editPresence {
                status = PresenceStatus.Online
                playing("on sickmc.net")
            }

            mainGuild = kord.getGuildOrThrow(Snowflake(config.getString("mainGuildID")))
            staffGuild = kord.getGuildOrThrow(Snowflake(config.getString("secondGuildID")))
            mainGuild.getApplicationCommands().collect { app -> app.delete() }
            val builder: RequestGuildMembersBuilder.() -> Unit = { requestAllMembers() }
            mainGuild.requestMembers(builder).collect { event -> event.members.forEach { it.load() } }
            kord.createGuildUserCommand(mainGuild.id, "add to ticket") {
                defaultMemberPermissions = Permissions(Permission.ManageMessages)
            }
            kord.createGuildMessageCommand(mainGuild.id, "update rank message") {
                defaultMemberPermissions = Permissions(Permission.Administrator)
            }
            kord.createGuildUserCommand(mainGuild.id, "restart bot") {
                defaultMemberPermissions = Permissions(Permission.Administrator)
            }

            registerTicketHandlers()
            registerWelcomeHandlers()
            registerLevelingHandlers()
            registerStaffModule()

            println("Bot has started")
        }

        kord.login {
            intents = Intents.all
        }
    }

}

suspend fun Member.load() {
    val result = leveling.findOne(LevelUser::snowflake eq id)
    levelingCache += if (result == null) {
        val levelUser = LevelUser(id, 0, arrayListOf(), null)
        leveling.insertOne(levelUser)
        levelUser
    } else {
        result
    }
}