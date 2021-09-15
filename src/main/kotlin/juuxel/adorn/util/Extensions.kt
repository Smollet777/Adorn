package juuxel.adorn.util

import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Property
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.Direction
import net.minecraft.util.registry.Registry

fun ItemStack.toTextWithCount(): Text =
    TranslatableText("text.adorn.item_stack_with_count", count, toHoverableText())

fun BlockState.withBlock(block: Block): BlockState =
    entries.entries.fold(block.defaultState) { acc, (key, value) ->
        @Suppress("UNCHECKED_CAST") // Cast to Comparable<Any>
        acc.with(key as Property<Comparable<Any>>, value as Comparable<Any>)
    }

/**
 * Gets the squared distance of this block entity to ([x], [y], [z]).
 *
 * Used to be in vanilla but was removed.
 */
fun BlockEntity.getSquaredDistance(x: Double, y: Double, z: Double): Double {
    val xd = pos.x + 0.5 - x
    val yd = pos.y + 0.5 - y
    val zd = pos.z + 0.5 - z
    return xd * xd + yd * yd + zd * zd
}

/**
 * Registers a [visitor] for this registry that will be called for each
 * entry currently in the registry, and all future entries.
 */
inline fun <A> Registry<A>.visit(crossinline visitor: (A) -> Unit) {
    this.forEach(visitor)

    RegistryEntryAddedCallback.event(this)
        .register(
            RegistryEntryAddedCallback { _, _, entry ->
                visitor(entry)
            }
        )
}

/**
 * Creates a safe copy of this block's settings.
 *
 * The safe copy does not have lambdas that reference this block directly.
 * Instead, the default state is used for the various lambdas.
 */
fun Block.copySettingsSafely(): AbstractBlock.Settings =
    FabricBlockSettings.of(defaultState.material)
        .luminance(defaultState.luminance)
        .apply { getHardness(defaultState)?.let(this::hardness) }
        .resistance(blastResistance)
        .velocityMultiplier(velocityMultiplier)
        .jumpVelocityMultiplier(jumpVelocityMultiplier)
        .slipperiness(slipperiness)
        .sounds(defaultState.soundGroup)

private fun getHardness(state: BlockState): Float? =
    try {
        state.getHardness(null, null)
    } catch (e: NullPointerException) {
        null
    }

fun Direction.Axis.turnHorizontally(): Direction.Axis =
    when (this) {
        Direction.Axis.X -> Direction.Axis.Z
        Direction.Axis.Z -> Direction.Axis.X
        Direction.Axis.Y -> Direction.Axis.Y
    }

fun Direction.Axis.getDirection(axisDirection: Direction.AxisDirection): Direction =
    when (axisDirection) {
        Direction.AxisDirection.POSITIVE -> when (this) {
            Direction.Axis.X -> Direction.EAST
            Direction.Axis.Y -> Direction.UP
            Direction.Axis.Z -> Direction.SOUTH
        }

        Direction.AxisDirection.NEGATIVE -> when (this) {
            Direction.Axis.X -> Direction.WEST
            Direction.Axis.Y -> Direction.DOWN
            Direction.Axis.Z -> Direction.NORTH
        }
    }
