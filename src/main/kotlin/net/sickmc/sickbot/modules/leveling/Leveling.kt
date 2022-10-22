package net.sickmc.sickbot.modules.leveling

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import dev.kord.core.entity.VoiceState
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.*
import net.sickmc.sickapi.util.databaseScope
import net.sickmc.sickbot.kord
import net.sickmc.sickbot.mainGuild
import net.sickmc.sickbot.utils.*
import org.litote.kmongo.eq
import kotlin.time.Duration.Companion.minutes

val levelingCache: ArrayList<LevelUser> = arrayListOf()
val usersToUpdate = arrayListOf<Snowflake>()

fun registerLevelingHandlers() {
    updater
    messageListener
    voiceJoinListener
    activeVoiceJob
    rankingMessageUpdater
    rankingMessageCreateHandler
    rankingMessageUpdateHandler
    rankingButtonHandler
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
private const val voicePoints = 1

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
    checkLevelChange(levelUser, message)
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
    checkLevelChange(levelUser, null)
}

private val activeVoiceJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
    while (true) {
        delay(5.minutes)
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
            checkLevelChange(levelUser, null)
        }
    }
}

private fun VoiceState.valid() = !isSelfDeafened && !isDeafened && !isMuted && !isSelfMuted && !isSuppressed

suspend fun checkLevelChange(user: LevelUser, message: Message?) {
    val previous = (user.points - 1).level()
    val current = user.points.level()
    if (current == previous) return
    if (message != null) {
        message.reply {
            generateMessage(user, previous, current)
        }
        return
    }
    config.getString("chat_channel").snowflake().channel<TextChannel>().createMessage {
        generateMessage(user, previous, current)
    }
}

private suspend fun UserMessageCreateBuilder.generateMessage(user: LevelUser, previous: Level, now: Level) {
    embed {
        title = "Level Up"
        description = """Congratulations <@${user.snowflake}> <a:party:959481387092676618>
            
            > **previous level**  <:${previous.emoji.name}:${previous.emoji.id}> ${previous.name}
            > **new level**  <:${now.emoji.name}:${now.emoji.id}> ${now.name}
            > **reward**  ${now.reward!!.name} :gift:
            
            *You can claim you reward in <#${config.getString("ranking_channel")}>*
        """.trimMargin()

        userFooter(user.snowflake.member())
        color(BotColors.Level)
    }
}