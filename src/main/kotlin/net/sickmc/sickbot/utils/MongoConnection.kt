package net.sickmc.sickbot.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.sickmc.sickbot.utils.FileUtils
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val databaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
private val credentials = FileUtils.getFileAsDocument("mongo")
val mongoClient = KMongo.createClient(MongoClientSettings.builder()
    .applyConnectionString(ConnectionString("mongodb://${credentials.getString("username")}:${credentials.getString("password")}@${credentials.getString("address")}:${credentials.getInteger("port")}/?authSource=${credentials.getString("databaseName")}"))
    .build()).coroutine
val db = mongoClient.getDatabase(credentials.getString("databaseName"))

val configColl: CoroutineCollection<Document> = db.getCollection("config")
val playerColl = db.getCollection<Document>("sickPlayers")
val discordPlayerColl: CoroutineCollection<Document> = db.getCollection("discordPlayer")
val ticketColl: CoroutineCollection<Document> = db.getCollection("ticket")
val rankGroupColl = db.getCollection<Document>("rankGroups")
val ranksColl = db.getCollection<Document>("ranks")
lateinit var config: Document