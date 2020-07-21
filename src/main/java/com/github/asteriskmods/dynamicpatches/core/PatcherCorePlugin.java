package com.github.asteriskmods.dynamicpatches.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

import javax.annotation.Nullable;
import java.util.Map;

@TransformerExclusions({ "com.github.asteriskmods.dynamicpatches.core" })
public class PatcherCorePlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.github.asteriskmods.dynamicpatches.core.PatcherClassTransformer" };
    }

    @Override
    public String getModContainerClass() {
        return "com.github.asteriskmods.dynamicpatches.core.ModContainer";
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        data.put("LZLoaderVersion", "1.0.0");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
