package fi.dy.masa.servux.scheduler.tasks;

import java.util.List;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.WorldUtils;
import fi.dy.masa.servux.util.game.EntityUtils;
import fi.dy.masa.servux.util.position.IntBoundingBox;

public class TaskFillArea extends TaskProcessChunkMultiPhase
{
    protected final BlockState fillState;
    @Nullable protected final BlockState replaceState;
    protected final boolean removeEntities;

    public TaskFillArea(TaskContext ctx,
                        final List<Box> boxes,
                        final BlockState fillState,
                        final @Nullable BlockState replaceState,
                        final boolean removeEntities)
    {
        super(ctx);

        this.fillState = fillState;
        this.replaceState = replaceState;
        this.removeEntities = removeEntities;
        this.addPerChunkBoxes(boxes);
    }

    @Override
    protected boolean canProcessChunk(ChunkPos pos)
    {
        return this.areSurroundingChunksLoaded(pos, this.context.level(), 0);
    }

    @Override
    public boolean execute(ProfilerFiller profiler)
    {
        return this.executeMultiPhase(profiler);
    }

    @Override
    protected void onNextChunkFetched(ChunkPos pos)
    {
        this.directFillBoxesInChunk(pos);
    }

    protected void directFillBoxesInChunk(ChunkPos pos)
    {
        for (IntBoundingBox box : this.getBoxesInChunk(pos))
        {
            this.directFillBox(box, this.removeEntities);
        }

        this.finishProcessingChunk(pos);
    }

    protected void directFillBox(IntBoundingBox box, boolean removeEntities)
    {
        ServerLevel level = this.context.level();

        if (removeEntities)
        {
            directRemoveEntities(box, level);
        }

        WorldUtils.setShouldPreventBlockUpdates(level, true);

        BlockState barrier = Blocks.BARRIER.defaultBlockState();
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (int z = box.minZ(); z <= box.maxZ(); ++z)
        {
            for (int x = box.minX(); x <= box.maxX(); ++x)
            {
                for (int y = box.maxY(); y >= box.minY(); --y)
                {
                    posMutable.set(x, y, z);
                    BlockState oldState = level.getBlockState(posMutable);

                    if ((this.replaceState == null && oldState != this.fillState) || oldState == this.replaceState)
                    {
                        BlockEntity te = level.getBlockEntity(posMutable);

                        if (te instanceof Container)
                        {
                            ((Container) te).clearContent();
                            level.setBlock(posMutable, barrier, 0x32);
                        }

                        level.setBlock(posMutable, this.fillState, 0x32);
                    }
                }
            }
        }

        WorldUtils.setShouldPreventBlockUpdates(level, false);
    }

    public static void directRemoveEntities(IntBoundingBox box, Level level)
    {
        AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
        List<Entity> entities = level.getEntities((Entity) null, aabb, EntityUtils.NOT_PLAYER);

        for (Entity entity : entities)
        {
            if ((entity instanceof Player) == false)
            {
                entity.discard();
            }
        }
    }

    @Override
    protected void onStop()
    {
        this.printCompletionMessage();
        this.sendTaskEndCommands();
        super.onStop();
    }

    protected void printCompletionMessage()
    {
        if (this.finished)
        {
            this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.fill_area.successful"));
        }
        else
        {
            this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.fill_area.interrupted"));
        }
    }
}
