package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.GardenCropMilestones.Companion.getCounter
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.CropMilestoneUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class CropSpeedMeter {
    private var display = listOf<String>()
    private var currentCrop: CropType? = null
    private var currentBlocks = 0
    private var tick = 0
    private var snapshot = listOf<String>()

    @SubscribeEvent
    fun onBlockBreak(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (startCrops.isEmpty()) return

        val blockState = event.getBlockState
        val cropBroken = blockState.getCropType() ?: return
        if (cropBroken.multiplier == 1) {
            if (blockState.isBabyCrop()) return
        }

        if (currentCrop != cropBroken) {
            currentCrop = cropBroken
            currentBlocks = 0
            snapshot = emptyList()
        }
        breakBlock()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (tick++ % 30 != 0) return

        updateDisplay()
    }

    private fun updateDisplay() {
        display = renderDisplay()
    }

    private fun renderDisplay(): MutableList<String> {
        val list = mutableListOf<String>()
        list.add("§7Crop Speed Meter")
        if (startCrops.isEmpty()) {
            list.add("§cOpen §e/cropmilestones §cto start!")
            return list
        }

        if (currentCrop == null) {
            list.add("§cStart breaking blocks!")
            return list
        }
        currentCrop?.let {
            list.add(" §7Current ${it.cropName} counter: §e${currentBlocks.addSeparators()}")
        }

        if (snapshot.isNotEmpty()) {
            list += snapshot
        } else {
            list.add("§cOpen §e/cropmilestones §cagain to calculate!")
        }

        return list
    }

    @SubscribeEvent
    fun onCropMilestoneUpdate(event: CropMilestoneUpdateEvent) {
        if (!isEnabled()) return
        val counters = mutableMapOf<CropType, Long>()
        for (cropType in CropType.values()) {
            counters[cropType] = cropType.getCounter()
        }
        if (startCrops.isEmpty()) {
            startCrops = counters
            currentCrop = null
            snapshot = emptyList()
        } else {
            currentCrop?.let {
                val crops = it.getCounter() - startCrops[it]!!
                val blocks = currentBlocks
                val cropsPerBlocks = (crops.toDouble() / blocks.toDouble()).round(3)

                val list = mutableListOf<String>()
                list.add("")
                list.add("§6Calculation results")
                list.add(" §7Crops collected: " + crops.addSeparators())
                list.add(" §7Blocks broken: " + blocks.addSeparators())
                list.add(" §7Crops per Block: " + cropsPerBlocks.addSeparators())

                val baseDrops = it.baseDrops
                val farmingFortune = (cropsPerBlocks * 100 / baseDrops).round(3)


                list.add(" §7Calculated farming Fortune: §e" + farmingFortune.addSeparators())
                list.add("§cOpen /cropmilestones again to recalculate!")

                snapshot = list
                updateDisplay()
            }
        }
    }

    private fun breakBlock() {
        currentBlocks++
    }

    companion object {
        var enabled = false
        private var startCrops = mapOf<CropType, Long>()

        fun toggle() {
            enabled = !enabled
            LorenzUtils.chat("§e[SkyHanni] Crop Speed Meter " + if (enabled) "§aEnabled" else "§cDisabled")
            startCrops = emptyMap()

        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        SkyHanniMod.feature.garden.cropSpeedMeterPos.renderStrings(display, posLabel = "Crop Speed Meter")
    }

    fun isEnabled() = enabled && GardenAPI.inGarden()
}