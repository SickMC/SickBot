package net.sickmc.sickbot.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

val databaseScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
val mongoClient = KMongo.createClient(
    MongoClientSettings.builder().applyConnectionString(
            ConnectionString(
                "mongodb://${System.getenv("MONGO_USERNAME")}:${System.getenv("MONGO_PASSWORD")}@${
                    System.getenv(
                        "MONGO_ADDRESS"
                    )
                }:${System.getenv("MONGO_PORT")}/?authSource=${System.getenv("MONGO_DATABASE")}"
            )
        ).build()
).coroutine
val db = mongoClient.getDatabase(System.getenv("MONGO_DATABASE"))

val configColl: CoroutineCollection<Document> = db.getCollection("config")
val ticketColl: CoroutineCollection<Document> = db.getCollection("ticket")
val rankGroupColl = db.getCollection<Document>("rankGroups")
val ranksColl = db.getCollection<Document>("ranks")
val levelingColl = db.getCollection<Document>("leveling")
val playerColl = db.getCollection<Document>("sickPlayers")
lateinit var config: Document

suspend fun reloadConfig() {
    config = configColl.findOne(Filters.eq("type", "discordbot")) ?: error("config document is null")
}