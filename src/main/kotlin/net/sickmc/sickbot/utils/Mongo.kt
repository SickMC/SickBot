package net.sickmc.sickbot.utils

import com.mongodb.client.model.Filters
import kotlinx.coroutines.runBlocking
import net.sickmc.sickapi.util.configs
import net.sickmc.sickapi.util.db
import net.sickmc.sickapi.util.getAndCreateCollection
import net.sickmc.sickbot.modules.leveling.LevelUser
import net.sickmc.sickbot.modules.ticket.Ticket
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineCollection

lateinit var config: Document
val leveling = runBlocking<CoroutineCollection<LevelUser>> {
    db.getAndCreateCollection("leveling")
}
val tickets = runBlocking<CoroutineCollection<Ticket>> {
    db.getAndCreateCollection("tickets")
}

suspend fun reloadConfig() {
    config = configs.findOne(Filters.eq("type", "discord")) ?: error("config document is null")
}