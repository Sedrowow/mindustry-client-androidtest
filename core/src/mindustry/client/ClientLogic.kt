package mindustry.client

import arc.*
import arc.util.*
import mindustry.*
import mindustry.client.ClientVars.*
import mindustry.client.antigrief.*
import mindustry.client.communication.*
import mindustry.client.navigation.*
import mindustry.client.ui.*
import mindustry.client.utils.*
import mindustry.game.*
import mindustry.gen.*
import mindustry.logic.*
import mindustry.world.blocks.logic.*

/** WIP client logic class, similar to [mindustry.core.Logic] but for the client.
 * Handles various events and such.
 * FINISHME: Move the 9000 different bits of code throughout the client to here */
class ClientLogic {
    /** Create event listeners */
    init {
        Events.on(EventType.ServerJoinEvent::class.java) { // Run when the player joins a server
            Main.setPluginNetworking(false)
            Navigation.stopFollowing()
            Spectate.pos = null
        }

        Events.on(EventType.WorldLoadEvent::class.java) { // Run when the world finishes loading (also when the main menu loads and on syncs)
            Core.app.post { syncing = false } // Run this next frame so that it can be used elsewhere safely
            if (!syncing) {
                Vars.player.persistPlans.clear() // FINISHME: Why is this even in the player class? It creates a queue for each player even tho only one is used...
                processorConfigs.clear()
            }
            lastJoinTime = Time.millis()
            PowerInfo.initialize()
            Navigation.obstacles.clear()
            configs.clear()
            Vars.control.input.lastVirusWarning = null
            dispatchingBuildPlans = false
            hidingBlocks = false
            hidingUnits = false
            hidingAirUnits = false
            showingTurrets = false
            if (Vars.state.rules.pvp) Vars.ui.announce("[scarlet]Don't use a client in pvp, it's uncool!", 5f)
            overdrives.clear()
            Client.tiles.clear()
        }

        Events.on(EventType.ClientLoadEvent::class.java) { // Run when the client finishes loading
            val changeHash = Core.files.internal("changelog").readString().hashCode() // Display changelog if the file contents have changed & on first run. (this is really scuffed lol)
            if (Core.settings.getInt("changeHash") != changeHash) ChangelogDialog.show()
            Core.settings.put("changeHash", changeHash)


            if (Core.settings.getBool("debug")) Log.level = Log.LogLevel.debug // Set log level to debug if the setting is checked
            if (Core.settings.getBool("discordrpc")) Vars.platform.startDiscord()
            if (Core.settings.getBool("mobileui")) Vars.mobile = !Vars.mobile
            if (Core.settings.getBool("viruswarnings")) LExecutor.virusWarnings = true;

            Autocomplete.autocompleters.add(BlockEmotes())
            Autocomplete.autocompleters.add(PlayerCompletion())
            Autocomplete.autocompleters.add(CommandCompletion())

            Autocomplete.initialize()

            Navigation.navigator.init()

            Core.settings.getBoolOnce("client730") { Core.settings.put("disablemonofont", true) } // FINISHME: Remove later

            if (OS.hasProp("policone")) { // People spam these and its annoying. add some argument to make these harder to find
                Client.register("poli", "Spelling is hard. This will make sure you never forget how to spell the plural of poly, you're welcome.") { _, _ ->
                    Call.sendChatMessage("Unlike a roly-poly whose plural is roly-polies, the plural form of poly is polys. Please remember this, thanks! :)")
                }

                Client.register("silicone", "Spelling is hard. This will make sure you never forget how to spell silicon, you're welcome.") { _, _ ->
                    Call.sendChatMessage("\"In short, silicon is a naturally occurring chemical element, whereas silicone is a synthetic substance.\" They are not the same, please get it right!")
                }
            }

            val encoded = Main.keyStorage.cert()?.encoded
            if (encoded != null && Main.keyStorage.builtInCerts.any { it.encoded.contentEquals(encoded) }) {
                Client.register("update <name>") { args, _ ->
                    val name = args[0]
                    val player = Groups.player.minByOrNull { Strings.levenshtein(it.name, name) } ?: return@register
                    Main.send(CommandTransmission(CommandTransmission.Commands.UPDATE, Main.keyStorage.cert() ?: return@register, player))
                }
            }
        }

        Events.on(EventType.PlayerJoin::class.java) { e -> // Run when a player joins the server
            if (e.player == null) return@on

            if (Core.settings.getBool("clientjoinleave") && (Vars.ui.chatfrag.messages.isEmpty || !Strings.stripColors(Vars.ui.chatfrag.messages.first().message).equals("${Strings.stripColors(e.player.name)} has connected.")) && Time.timeSinceMillis(lastJoinTime) > 10000)
                Vars.player.sendMessage(Core.bundle.format("client.connected", e.player.name))
        }

        Events.on(EventType.PlayerLeave::class.java) { e -> // Run when a player leaves the server
            if (e.player == null) return@on

            if (Core.settings.getBool("clientjoinleave") && (Vars.ui.chatfrag.messages.isEmpty || !Strings.stripColors(Vars.ui.chatfrag.messages.first().message).equals("${Strings.stripColors(e.player.name)} has disconnected.")))
                Vars.player.sendMessage(Core.bundle.format("client.disconnected", e.player.name))
        }

        Events.on(EventType.BlockBuildEndEvent::class.java) { e -> // Configure logic after construction
            if (e.unit == null || e.team != Vars.player.team() || !Core.settings.getBool("processorconfigs")) return@on
            val build = e.tile.build as? LogicBlock.LogicBuild ?: return@on
            val packed = e.tile.pos()
            Log.info("${e.tile.pos()} ${!processorConfigs.containsKey(packed)} ${build.code.any()} ${build.links.any()}\n${processorConfigs.get(packed)}")
            if (!processorConfigs.containsKey(packed) || build.code.any() || build.links.any()) return@on

            configs.add(ConfigRequest(e.tile.x.toInt(), e.tile.y.toInt(), processorConfigs.remove(packed)))
        }
    }
}