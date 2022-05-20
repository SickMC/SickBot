package net.sickmc.sickbot

import com.mongodb.client.model.Filters
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.Kord
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.configColl

class Startup {

    companion object{
        lateinit var instance: Startup
    }

    init {
        instance = this
    }

    suspend fun start(){
        config = configColl.findOne(Filters.eq("type", "discordbot")) ?: error("config document is null")

        kord = Kord(config.getString("token")){
            stackTraceRecovery = true
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
                roles{cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                }
                channels { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                }
                voiceState { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                }
                emojis { cache, description ->
                    MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
                }
            }
        }
        SickBot().setupBot()
    }

}