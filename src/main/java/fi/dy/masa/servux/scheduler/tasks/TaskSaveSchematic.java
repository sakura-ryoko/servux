package fi.dy.masa.servux.scheduler.tasks;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import fi.dy.masa.servux.scheduler.TaskContext;
import fi.dy.masa.servux.schematic.LitematicaSchematic;
import fi.dy.masa.servux.schematic.selection.AreaSelection;
import fi.dy.masa.servux.schematic.selection.Box;
import fi.dy.masa.servux.util.StringUtils;
import fi.dy.masa.servux.util.IntBoundingBox;
import fi.dy.masa.servux.util.position.PositionUtils;

public class TaskSaveSchematic extends TaskProcessChunkBase
{
	private final LitematicaSchematic schematic;
	private final BlockPos origin;
	private final ImmutableMap<@NotNull String, @NotNull Box> subRegions;
	private final Set<UUID> existingEntities = new HashSet<>();
	@Nullable private final Path dir;
	@Nullable private final String fileName;
	private final LitematicaSchematic.SchematicSaveInfo info;
	private final boolean overrideFile;

	public TaskSaveSchematic(TaskContext context, LitematicaSchematic schematic, AreaSelection area, LitematicaSchematic.SchematicSaveInfo info)
	{
		this(context, null, null, schematic, area, info, false);
	}

	public TaskSaveSchematic(TaskContext context, @Nullable Path dir, @Nullable String fileName, LitematicaSchematic schematic, AreaSelection area, LitematicaSchematic.SchematicSaveInfo info, boolean overrideFile)
	{
		super(context);

		this.dir = dir;
		this.fileName = fileName;
		this.schematic = schematic;
		this.origin = area.getEffectiveOrigin();
		this.subRegions = area.getAllSubRegions();
		this.info = info;
		this.overrideFile = overrideFile;

		this.addPerChunkBoxes(area.getAllSubRegionBoxes());
	}

	@Override
	protected boolean canProcessChunk(ChunkPos pos)
	{
		return this.areSurroundingChunksLoaded(pos, this.context.level(), 0);
	}

	@Override
	protected boolean processChunk(ChunkPos pos)
	{
		Level world = this.context.level();
		ImmutableMap<@NotNull String, @NotNull IntBoundingBox> volumes = PositionUtils.getBoxesWithinChunk(pos.x, pos.z, this.subRegions);
		this.schematic.takeBlocksFromWorldWithinChunk(world, volumes, this.subRegions, this.info);

		if (this.info.ignoreEntities == false)
		{
			this.schematic.takeEntitiesFromWorldWithinChunk(world, pos.x, pos.z, volumes, this.subRegions, this.existingEntities, this.origin);
		}

		return true;
	}

	@Override
	protected void onStop()
	{
		if (this.finished)
		{
			final long time = System.currentTimeMillis();

			this.schematic.getMetadata().setTimeCreated(time);
			this.schematic.getMetadata().setTimeModified(time);
			this.schematic.getMetadata().setTotalBlocks(this.schematic.getTotalBlocksReadFromWorld());

			if (this.dir != null)
			{
				if (this.schematic.writeToFile(this.dir, this.fileName, this.overrideFile))
				{
					this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.save.successful", this.fileName));
				}
				else
				{
					this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.save.failed", this.fileName));
				}
			}
		}
		else
		{
			this.context.listener().addFeedback(StringUtils.translate("servux.scheduler.task.save.interrupted"));
		}

		super.onStop();
	}
}
