package academy.hekiyou.fluidinspector.jade;

import academy.hekiyou.fluidinspector.FluidInspectorMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadeIntegration implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FluidRateComponentProvider.INSTANCE, BlockEntity.class);
        FluidInspectorMod.LOGGER.info("Registered server-side provider");
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(FluidRateComponentProvider.INSTANCE, Block.class);
        FluidInspectorMod.LOGGER.info("Registered client-side provider");
    }

}
