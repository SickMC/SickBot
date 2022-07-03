package net.sickmc.sickbot.modules

object ModuleHandler {
    suspend fun register() {
        Log.register()
        Welcome.register()
        Tickets.register()
        Moderation.register()
        Leveling.register()
        Staff.register()
    }

}
