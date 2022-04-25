package net.sickmc.sickbot.utils

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.launch

object RoleIDs {

    val ids = hashMapOf<String, Snowflake>()
    val privateIds = hashMapOf<String, Snowflake>()

    init {
        validateIDs()
    }

    fun getId(name: String): Snowflake?{
        return ids[name]
    }

    fun getPrivateId(name: String): Snowflake?{
        return privateIds[name]
    }

    fun validateIDs(){
        databaseScope.launch {
            rankGroupColl.find().toList().forEach {
                ids[it.getString("rankgroup")] = Snowflake(it.getString("discordRoleID"))
            }
            ranksColl.find().toList().forEach {
                privateIds[it.getString("rank")] = Snowflake(it.getString("discordID"))
            }
        }
    }

}