package com.tz.statpatterns.core;

import java.util.List;
import zone.rong.mixinbooter.ILateMixinLoader;

public final class LateMixinLoader implements ILateMixinLoader {
    @Override public List<String> getMixinConfigs() { return List.of("mixins.statpatterns.json"); }
}
