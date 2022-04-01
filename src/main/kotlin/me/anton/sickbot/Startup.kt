package me.anton.sickbot

import com.mongodb.client.model.Filters
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.Kord
import me.anton.sickbot.utils.MongoConnection
import org.bson.Document

class Startup {

    companion object{
        lateinit var instance: Startup
    }

    init {
        instance = this
    }

    lateinit var document: Document
    lateinit var bot: Kord
    lateinit var rankDocuments: List<Document>

    suspend fun start(){
        val connection = MongoConnection()
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
        SickBot().setupBot()
    }

}