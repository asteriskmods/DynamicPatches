package com.github.asteriskmods.dynamicpatches.core;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;
import org.spongepowered.asm.launch.MixinBootstrap;

public class ModContainer extends DummyModContainer {

    public ModContainer() {
        super(new ModMetadata());
        ModMetadata metadata = getMetadata();
        metadata.modId = "dynamicpatches";
        metadata.name = "DynamicPatches";
        metadata.description = "My private patches for thirdparty mods";
        metadata.version = "1.0.0";
        metadata.credits = "";
        metadata.authorList = ImmutableList.of("azure");
        setEnabledState(true);
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

}
