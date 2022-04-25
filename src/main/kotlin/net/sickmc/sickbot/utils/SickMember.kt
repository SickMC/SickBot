package net.sickmc.sickbot.utils

import com.mongodb.client.model.Filters
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.launch
import org.bson.Document
import java.util.UUID

class SickMember(val id: Snowflake, val document: Document?) {

    val mainDiscordRoleID = validateRoleID(true)
    val secondDiscordRoleID = validateRoleID(false)
    val uuid: UUID? = checkVerified()
    val verified = uuid != null

    private fun validateRoleID(main: Boolean): Snowflake?{
        if (document == null)return null
        var snowflake: Snowflake? = null
        databaseScope.launch {
            snowflake = if (main){
                val rankDoc: Document = ranksColl.findOne(Filters.eq("rank", document.getString("rank"))) ?: error("Rank was invalid from member $id")
                val rankGroupDoc: Document = rankGroupColl.findOne(Filters.eq("rankgroup", rankDoc.getString("parent"))) ?: error("Rankgroup was invalid from member $id")
                Snowflake(rankGroupDoc.getString("discordRoleID"))
            }else{
                val rankDoc: Document = ranksColl.findOne(Filters.eq("rank", document.getString("rank"))) ?: error("Rank was invalid from member $id")
                Snowflake(rankDoc.getString("discordID"))
            }
        }
        return snowflake
    }

    private fun checkVerified(): UUID?{
        var uuid:UUID? = null
        databaseScope.launch {
            val document: Document? = playerColl.findOne(Filters.eq("discordID", id.toString()))
            if (document == null) uuid = null
            uuid = UUID.fromString(document?.getString("uuid"))
        }
        return uuid
    }

}

object SickMembers{
    val members = hashMapOf<Snowflake, SickMember>()

    fun getCachedSickMember(snowflake: Snowflake): SickMember? {
        return members[snowflake]
    }

    suspend fun getSickMember(snowflake: Snowflake): SickMember {
        if (members.contains(snowflake)) return members[snowflake]!!
        var memberDoc: Document? = null
        memberDoc = playerColl.findOne(Filters.eq("discordID", snowflake.toString()))
        return SickMember(snowflake, memberDoc)
    }

    suspend fun reloadMember(snowflake: Snowflake) {
        if (members.contains(snowflake)) members.remove(snowflake)
        getSickMember(snowflake)
    }

}