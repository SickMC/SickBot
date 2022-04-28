package net.sickmc.sickbot.modules

import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.on
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.SickMembers
import net.sickmc.sickbot.utils.databaseScope

object MemberHandler {
    fun register(){
        handleMembers()
    }
    private fun handleMembers(){
        databaseScope.launch {
            val members = mainGuild.members.toList()
            members.forEach {
                SickMembers.getSickMember(it.id)
            }
        }
        kord.on<MemberJoinEvent> {
            val snowflake = this.member.asUser().id
            databaseScope.launch {
                SickMembers.getSickMember(snowflake)
            }
        }
    }

}