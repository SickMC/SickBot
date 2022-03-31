package me.anton.sickbot.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import me.anton.sickbot.utils.FileUtils
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoConnection {

    private val credentials = FileUtils.getFileAsDocument("mongo")
    private val uri = "mongodb://${credentials.getString("username")}:${credentials.getString("password")}@${credentials.getString("address")}:${credentials.getInteger("port")}/?authSource=${credentials.getString("databaseName")}"
    private val settings = MongoClientSettings.builder().applyConnectionString(ConnectionString(uri)).build()

    val client: CoroutineClient = KMongo.createClient(settings).coroutine

    val db = client.getDatabase(credentials.getString("databaseName"))
    val configColl: CoroutineCollection<Document> = db.getCollection("config")
    val discordPlayerColl: CoroutineCollection<Document> = db.getCollection("discordPlayer")

}