package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class WikiManager {

    private val config get() = SkyHanniMod.feature.commands.fandomWiki
    private val urlPrefix = "https://hypixel-skyblock.fandom.com/wiki/"
    private val urlSearchPrefix = "${urlPrefix}Special:Search?query="

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.useFandomWiki")
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return

        val packet = event.packet

        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (!(message.startsWith("/wiki"))) return
            event.isCanceled = true
            if (message == "/wiki") {
                LorenzUtils.chat("§e[SkyHanni] Opening the Fandom Wiki..")
                OSUtils.openBrowser("${urlPrefix}/Hypixel_SkyBlock_Wiki")
            } else if (message.startsWith("/wiki ") || message == ("/wikithis")) { //conditional to see if we need Special:Search page
                if (message == ("/wikithis")) {
                    val itemInHand = InventoryUtils.getItemInHand() ?: return
                    wikiTheItem(itemInHand)
                } else {
                    val search = packet.message.split("/wiki ").last()
                    LorenzUtils.chat("§e[SkyHanni] Searching the Fandom Wiki for §a$search")
                    val wikiUrlCustom = "$urlSearchPrefix$search&scope=internal"
                    OSUtils.openBrowser(wikiUrlCustom.replace(' ', '+'))
                }
            }
        }
    }

    @SubscribeEvent
    fun onKeyClickWithTooltipActive(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (Minecraft.getMinecraft().currentScreen == null) return
        if (NEUItems.neuHasFocus()) return //because good heavens if this worked on neuitems...

        if (Keyboard.isKeyDown(config.fandomWikiKeybind)) {
            val itemUnderCursor = event.itemStack ?: return
            wikiTheItem(itemUnderCursor)
        }
    }

    private fun wikiTheItem(item: ItemStack) {
        var wikiUrlSearch = ""
        val itemDisplayName = (item.nameWithEnchantment ?: return).replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString() ?: return
        LorenzUtils.chat("§e[SkyHanni] Searching the Fandom Wiki for §a$itemDisplayName")
        if (internalName != "NONE") wikiUrlSearch = "$urlSearchPrefix$internalName&scope=internal"
        else wikiUrlSearch = "$urlSearchPrefix${itemDisplayName.removeColor()}&scope=internal"
        OSUtils.openBrowser(wikiUrlSearch.replace(' ', '+'))
    }

    private fun isEnabled() = config.useFandomWiki
}