package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.VoiceState
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.*
import net.sickmc.sickapi.util.databaseScope
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.config
import net.sickmc.sickbot.utils.leveling
import net.sickmc.sickbot.utils.snowflake
import org.litote.kmongo.eq
import kotlin.time.Duration.Companion.minutes

val levelingCache: ArrayList<LevelUser> = arrayListOf()
val usersToUpdate = arrayListOf<Snowflake>()

fun registerLevelingHandlers() {
    updater
    messageListener
    voiceJoinListener
    activeVoiceJob
}

private val updater = databaseScope.launch {
    while (true) {
        levelingCache.filter { usersToUpdate.contains(it.snowflake) }.forEach {
            leveling.replaceOne(LevelUser::snowflake eq it.snowflake, it)
        }
        usersToUpdate.clear()
        delay(1.minutes)
    }
}

private val messageCooldowns = hashMapOf<Snowflake, Long>()
private val voiceCooldowns = hashMapOf<Snowflake, Long>()
private val voiceCooldown = 5.minutes.inWholeMilliseconds
private val messageCooldown = 1.minutes.inWholeMilliseconds
private const val messagePoints = 1
private const val voicePoints = 3

private val messageListener = kord.on<MessageCreateEvent> {
    if (message.author?.isBot == true) return@on
    if (message.getGuild().id != mainGuild.id) return@on
    if (config.getList("ignoredLevelingChannels", String::class.java).map { it.snowflake() }
            .contains(message.channelId)) return@on
    if (message.author == null) return@on
    if (messageCooldowns.containsKey(message.author!!.id) && messageCooldowns[message.author!!.id]!! > System.currentTimeMillis()) return@on
    val levelUser = levelingCache.find { it.snowflake == message.author!!.id }!!
    levelUser.points = levelUser.points.plus(messagePoints)
    messageCooldowns[message.author!!.id] = System.currentTimeMillis() + messageCooldown
    if (!usersToUpdate.contains(levelUser.snowflake)) usersToUpdate.add(levelUser.snowflake)
}

private val voiceJoinListener = kord.on<VoiceStateUpdateEvent> {
    if (!this.state.valid()) return@on
    val member = state.getMember()
    if (member.isBot) return@on
    if (state.guildId != mainGuild.id) return@on
    if (config.getList("ignoredLevelingChannels", String::class.java).map { it.snowflake() }
            .contains(state.channelId)) return@on
    if (!voiceCooldowns.containsKey(member.id)) voiceCooldowns[member.id] = System.currentTimeMillis() + voiceCooldown
    else if (voiceCooldowns[member.id]!! > System.currentTimeMillis()) return@on
    val levelUser = levelingCache.find { it.snowflake == member.id }!!
    levelUser.points = levelUser.points.plus(voicePoints)
    voiceCooldowns[member.id] = System.currentTimeMillis() + voiceCooldown
    if (!usersToUpdate.contains(levelUser.snowflake)) usersToUpdate.add(levelUser.snowflake)
}

private val activeVoiceJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
    while (true) {
        mainGuild.voiceStates.collect {
            if (!it.valid()) return@collect
            val member = it.getMember()
            if (member.isBot) return@collect
            if (config.getList("ignoredLevelingChannels", String::class.java).map { channel -> channel.snowflake() }
                    .contains(it.channelId)) return@collect
            if (!voiceCooldowns.containsKey(member.id)) voiceCooldowns[member.id] =
                System.currentTimeMillis() + voiceCooldown
            else if (voiceCooldowns[member.id]!! > System.currentTimeMillis()) return@collect
            val levelUser = levelingCache.find { levelUser -> levelUser.snowflake == member.id }!!
            levelUser.points = levelUser.points.plus(voicePoints)
            voiceCooldowns[member.id] = System.currentTimeMillis() + voiceCooldown
            if (!usersToUpdate.contains(levelUser.snowflake)) usersToUpdate.add(levelUser.snowflake)
        }
    }
}

private fun VoiceState.valid() = !isSelfDeafened && !isDeafened && !isMuted && !isSelfMuted && !isSuppressed