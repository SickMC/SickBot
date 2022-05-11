package net.sickmc.sickbot.modules

import com.mongodb.client.model.Filters
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.on
import dev.kord.rest.builder.message.modify.embed
import dev.kord.x.emoji.Emojis
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.EmbedVariables
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.levelingColl
import net.sickmc.sickbot.utils.*
import org.bson.Document
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Leveling {

    private val messageCooldowns = hashMapOf<Member, Long>()
    private val voiceCooldowns = hashMapOf<Member, Long>()
    private val players = hashMapOf<Member, Document>()
    private val refreshCooldown = Clock.System.now().toEpochMilliseconds() + 30.seconds.inWholeMilliseconds
    private val ignoredVoiceChannels = arrayListOf<Snowflake>()
    private val ignoredMessageChannels = arrayListOf<Snowflake>()
    private val levelingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    suspend fun register() {
        handleMessages()
        handleVoiceJoin()
        handleActiveVoice()
        handleRanking()
        handleRankMessage()
        updater()
    }

    private suspend fun updater() {
        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                players.forEach {
                    levelingColl.replaceOne(Filters.eq("id", it.key.id.toString()), it.value)
                }
            }
        }
    }

    private fun handleMessages() {
        kord.on<MessageCreateEvent> {
            if (message.getGuild() != mainGuild) return@on
            if (message.author == null) return@on
            if (message.author!!.isBot) return@on
            if (ignoredMessageChannels.contains(message.channel.id)) return@on
            val member = message.getAuthorAsMember()!!
            if (!messageCooldowns.containsKey(member)) messageCooldowns[member] =
                Clock.System.now().toEpochMilliseconds() + 1.minutes.inWholeMilliseconds
            if (messageCooldowns[member]!! < Clock.System.now().toEpochMilliseconds()) return@on
            check(member)
        }
    }

    private fun handleVoiceJoin() {
        kord.on<VoiceStateUpdateEvent> {
            if (state.getMember().isBot) return@on
            if (state.getGuild() != mainGuild) return@on
            if (ignoredVoiceChannels.contains(state.channelId)) return@on
            val member = state.getMember()
            if (!voiceCooldowns.containsKey(state.getMember())) voiceCooldowns[member] =
                Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
            if (voiceCooldowns[member]!! < Clock.System.now().toEpochMilliseconds()) return@on
            check(member)
        }
    }

    private suspend fun handleActiveVoice() {
        val states = mainGuild.voiceStates.filter {
            !it.isDeafened && !it.isSelfDeafened && !it.isMuted && !it.isSelfMuted && !it.isSuppressed && !ignoredVoiceChannels.contains(
                it.channelId
            )
        }.toList()

        levelingScope.launch {
            while (true) {
                delay(5.minutes)
                states.forEach {
                    val member = it.getMember()
                    if (!voiceCooldowns.containsKey(member)) voiceCooldowns[member] =
                        Clock.System.now().toEpochMilliseconds() + 5.minutes.inWholeMilliseconds
                    if (voiceCooldowns[member]!! < Clock.System.now().toEpochMilliseconds()) return@forEach
                    check(member, increment = 2)
                }
            }
        }
    }

    private suspend fun handleRanking() {
        val channel = mainGuild.getChannel(Snowflake(config.getString("rankingChannel"))) as MessageChannel
        val message = channel.getMessage(Snowflake(config.getString("rankingMessage")))
        levelingScope.launch {
            while (true) {
                delay(10.minutes)
                val filteredPlayers = hashMapOf<Member, Document>()
                levelingColl.find().toList().forEach {
                    try {
                        val member = mainGuild.getMember(Snowflake(it.getString("id")))
                        filteredPlayers[member] = it
                    } catch (e: EntityNotFoundException) {
                        return@forEach
                    }
                }
                val sorted = filteredPlayers.entries.sortedBy { it.value.getInteger("points") }
                    .map { it.key }.reversed()
                message.edit {
                    embed {
                        title = EmbedVariables.title("Ranking")
                        timestamp = Clock.System.now()
                        footer = EmbedVariables.userFooter(sorted[0])
                        color = EmbedVariables.color()
                        description = """
                            > **1.** ${sorted[0].mention} - ${filteredPlayers[sorted[0]]?.getInteger("points")} ${Emojis.smallBlueDiamond}
                            > **2.** ${sorted[1].mention} - ${filteredPlayers[sorted[1]]?.getInteger("points")} ${Emojis.smallBlueDiamond}
                            > **3.** ${sorted[2].mention} - ${filteredPlayers[sorted[2]]?.getInteger("points")} ${Emojis.smallBlueDiamond}
                            > **4.** ${sorted[3].mention} - ${filteredPlayers[sorted[3]]?.getInteger("points")} ${Emojis.smallBlueDiamond}
                            > **5.** ${sorted[4].mention} - ${filteredPlayers[sorted[4]]?.getInteger("points")} ${Emojis.smallBlueDiamond}
                            *updates every 10 minutes*
                        """.trimIndent()
                    }
                }
            }
        }
    }

    private fun handleRankMessage() {
        kord.on<MessageCreateEvent> {
            if (message.content != "!ranking") return@on
            if (message.getGuildOrNull() == null) return@on
            if (message.author?.isBot == true) return@on
            if (!message.author?.asMember(mainGuild.id)?.roleIds?.contains(RoleIDs.getId("Administration"))!!) return@on
            config.replace("rankingMessage", message.getChannel().createMessage {
                embed {
                    title = EmbedVariables.title("Ranking")
                    timestamp = Clock.System.now()
                    footer = EmbedVariables.selfFooter()
                    color = EmbedVariables.color()
                    description = "Coming soon"
                }
            }.id.toString())
            configColl.replaceOne(Filters.eq("type", "discordbot"), config)
            message.delete("Ranking was send!")
        }
    }

    private suspend fun check(member: Member, increment: Int = 1) {
        if (!players.containsKey(member)) {
            var doc = levelingColl.findOne(Filters.eq("id", member.id.toString()))
            if (doc == null) {
                doc = Document("id", member.id.toString()).append("points", increment)
                levelingColl.insertOne(doc)
            }
            players[member] = doc ?: error("document was null in Leveling Module - ${member.id}")
        } else {
            players[member]?.replace("points", players[member]?.getInteger("points")?.plus(increment))
            if (refreshCooldown < Clock.System.now().toEpochMilliseconds()) {
                levelingColl.replaceOne(
                    Filters.eq("id", member.id.toString()),
                    players[member] ?: error("${member.id} was not in players")
                )
            }
        }
    }

}




