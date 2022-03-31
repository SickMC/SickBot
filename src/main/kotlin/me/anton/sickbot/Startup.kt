package me.anton.sickbot

import com.mongodb.client.model.Filters
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.Kord
import kotlinx.coroutines.runBlocking
import me.anton.sickbot.database.MongoConnection
import me.anton.sickbot.utils.FileUtils
import org.bson.Document
import java.util.Arrays

class Startup {

    companion object{
        lateinit var instance: Startup
    }

    init {
        instance = this
        start()
    }

    lateinit var document: Document
    lateinit var bot: Kord
    lateinit var rankDocuments: List<Document>

    private fun start(){
        val connection = MongoConnection()
        runBlocking {
            connection.createConnection()
        }
        runBlocking {
            document = connection.configColl.findOne(Filters.eq("type", "discordbot"))!!
            rankDocuments = listOf(
                connection.configColl.findOne(Filters.eq("type", "ranks"))!!,
                connection.configColl.findOne(Filters.eq("type", "rankgroups"))!!)

            bot = Kord(document.getString("token")){
                cache {
                    users { cache, description ->
                        MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                    }
                    messages { cache, description ->
                        MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 100))
                    }
                    members { cache, description ->
                        MapEntryCache(cache, description, MapLikeCollection.none())
                    }
                    guilds { cache, description ->
                        MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                    }
                }
            }
            SickBot()
        }
    }

}