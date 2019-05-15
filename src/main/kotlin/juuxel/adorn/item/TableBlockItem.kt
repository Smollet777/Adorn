package juuxel.adorn.item

import io.github.juuxel.polyester.block.PolyesterBlock
import io.github.juuxel.polyester.item.PolyesterItem
import net.minecraft.block.CarpetBlock
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemUsageContext
import net.minecraft.util.ActionResult
import net.minecraft.util.math.Direction

class TableBlockItem(block: PolyesterBlock, settings: Settings) : BlockItem(block.unwrap(), settings), PolyesterItem {
    override val name = block.name

    override fun useOnBlock(context: ItemUsageContext): ActionResult {
        val world = context.world
        val pos = context.blockPos
        if (context.facing == Direction.UP && world.getBlockState(pos).block is CarpetBlock) {
            place(CarpetedTopPlacementContext(context))
            return ActionResult.SUCCESS
        }

        return super.useOnBlock(context)
    }
}