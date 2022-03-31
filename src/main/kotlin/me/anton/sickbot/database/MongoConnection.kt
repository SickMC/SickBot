package me.anton.sickbot.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.anton.sickbot.utils.FileUtils
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MongoConnection {

    private val scope = CoroutineScope(Dispatchers.Default)
    lateinit var client: CoroutineClient
    lateinit var configColl: CoroutineCollection<org.bson.Document>
    lateinit var discordPlayerColl: CoroutineCollection<org.bson.Document>

    fun createConnection(){
        val credentials = FileUtils.getFileAsDocument("mongo")

        scope.launch {
            val uri = "mongodb://${credentials.getString("username")}:${credentials.getString("password")}@${credentials.getString("address")}:${credentials.getInteger("port")}/?authSource=${credentials.getString("databaseName")}"
            val connectionString = ConnectionString(uri)
            val mongoSettings = MongoClientSettings.builder().applyConnectionString(connectionString).build()
            client = KMongo.createClient(mongoSettings).coroutine
            val db = client.getDatabase(credentials.getString("databaseName"))
            configColl = db.getCollection("config")
            discordPlayerColl = db.getCollection("discordPlayer")
        }
    }

}