package net.sickmc.sickbot

import com.mongodb.client.model.Filters
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.Kord
import net.sickmc.sickapi.util.configs
import net.sickmc.sickbot.utils.config

suspend fun main() {
    config = configs.findOne(Filters.eq("type", "discord")) ?: error("config document is null")

    kord = Kord(config.getString("token")) {
        stackTraceRecovery = true
        cache {
            users { cache, description ->
                MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
            }
            messages { cache, description ->
                MapEntryCache(cache, description, MapLikeCollection.lruLinkedHashMap(maxSize = 1000))
            }
            members { cache, description ->
                MapEntryCache(cache, description, MapLikeCollection.none())
            }
            guilds { cache, description ->
                MapEntryCache(cache, description, MapLikeCollection.concurrentHashMap())
            }
            roles { cache, description ->
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
    SickBot.setupBot()
}