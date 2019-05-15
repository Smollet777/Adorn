package juuxel.adorn.block

import io.github.juuxel.polyester.block.PolyesterBlockEntityType
import io.github.juuxel.polyester.block.PolyesterBlockWithEntity
import juuxel.adorn.block.entity.TradingStationBlockEntity
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityContext
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.state.StateFactory
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.ItemScatterer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World

class TradingStationBlock : PolyesterBlockWithEntity(Settings.copy(Blocks.CRAFTING_TABLE)), SneakClickHandler {
    override val name = "trading_station"
    override val itemSettings = Item.Settings().itemGroup(ItemGroup.DECORATIONS)
    override val blockEntityType = BLOCK_ENTITY_TYPE

    override fun appendProperties(builder: StateFactory.Builder<Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(AXIS)
    }

    override fun getPlacementState(context: ItemPlacementContext) =
        super.getPlacementState(context)!!.with(AXIS, context.playerHorizontalFacing.rotateYClockwise().axis)

    override fun onPlaced(world: World, pos: BlockPos, state: BlockState, entity: LivingEntity?, stack: ItemStack?) {
        if (entity is PlayerEntity) {
            val be = world.getBlockEntity(pos) as? TradingStationBlockEntity ?: return
            be.setOwner(entity)
        }
    }

    override fun activate(
        state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hitResult: BlockHitResult?
    ): Boolean {
        val be = world.getBlockEntity(pos)
        if (be is TradingStationBlockEntity) {
            if (!world.isClient && be.owner == null) {
                be.setOwner(player)
            }

            if (!world.isClient && !be.isOwner(player)) {
                val handStack = player.getStackInHand(hand)
                val trade = be.trade
                // TODO: Investigate buying damaged swords
                val validPayment = handStack.isEqualIgnoreTags(trade.price) &&
                        handStack.amount >= trade.price.amount &&
                        handStack.tag == trade.price.tag
                val canInsertPayment = be.storage.canInsert(trade.price)

                if (trade.isEmpty()) {
                    player.addChatMessage(TranslatableComponent("block.adorn.trading_station.empty_trade"), true)
                } else if (!be.isStorageStocked()) {
                    player.addChatMessage(TranslatableComponent("block.adorn.trading_station.storage_not_stocked"), true)
                } else if (!canInsertPayment) {
                    player.addChatMessage(TranslatableComponent("block.adorn.trading_station.storage_full"), true)
                } else if (validPayment) {
                    handStack.subtractAmount(trade.price.amount)
                    player.giveItemStack(trade.selling.copy())
                    be.storage.tryExtract(trade.selling)
                    be.storage.tryInsert(trade.price)
                }
            } else {
                player.openContainer(state.createContainerProvider(world, pos))
            }
        }

        return true
    }

    override fun onSneakClick(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity): ActionResult {
        val be = world.getBlockEntity(pos) as? TradingStationBlockEntity ?: return ActionResult.PASS

        // Show customer GUI
        if (!be.isOwner(player)) {
            player.openContainer(state.createContainerProvider(world, pos))
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    override fun onBlockRemoved(state1: BlockState, world: World, pos: BlockPos, state2: BlockState, b: Boolean) {
        if (state1.block != state2.block) {
            val entity = world.getBlockEntity(pos)

            if (entity is TradingStationBlockEntity) {
                ItemScatterer.spawn(world, pos, entity.storage)
                world.updateHorizontalAdjacent(pos, this)
            }

            super.onBlockRemoved(state1, world, pos, state2, b)
        }
    }

    override fun isFullBoundsCubeForCulling(state: BlockState?) = false

    companion object {
        val BLOCK_ENTITY_TYPE = PolyesterBlockEntityType(::TradingStationBlockEntity)
        val AXIS = Properties.AXIS_XZ
    }
}