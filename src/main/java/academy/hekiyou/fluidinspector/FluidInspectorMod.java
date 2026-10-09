package academy.hekiyou.fluidinspector;

import academy.hekiyou.fluidinspector.bytecode.FluidHandlerBytecodeService;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Objects;

@Mod(FluidInspectorMod.MOD_ID)
public class FluidInspectorMod {

    public static final String MOD_ID = "fluidinspector";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation RESOURCE_LOC = new ResourceLocation(MOD_ID, "fluid_inspector");

    private static FluidInspectorMod INSTANCE;

    private final Stats stats = new Stats();

    public FluidInspectorMod() {
        INSTANCE = this;
    }

    public static Stats stats() {
        return INSTANCE.stats;
    }

}
