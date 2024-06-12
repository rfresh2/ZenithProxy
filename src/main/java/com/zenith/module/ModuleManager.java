package com.zenith.module;

import com.zenith.module.impl.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import java.util.List;

import static java.util.Arrays.asList;

public class ModuleManager {
    private final Reference2ObjectMap<Class<? extends Module>, Module> moduleClassMap = new Reference2ObjectOpenHashMap<>();

    public void init() {
        asList(
            new ActionLimiter(),
            new ActiveHours(),
            new AntiAFK(),
            new AntiKick(),
            new AntiLeak(),
            new AutoArmor(),
            new AutoDisconnect(),
            new AutoEat(),
            new AutoFish(),
            new AutoMend(),
            new AutoReconnect(),
            new AutoReply(),
            new AutoRespawn(),
            new AutoTotem(),
            new ChatHistory(),
            new ESP(),
            new KillAura(),
            new PlayerSimulation(),
            new ReplayMod(),
            new Spammer(),
            new Spook(),
            new VisualRange()
        ).forEach(m -> {
            addModule(m);
            m.syncEnabledFromConfig();
        });
    }

    private void addModule(Module module) {
        moduleClassMap.put(module.getClass(), module);
    }

    public <T extends Module> T get(final Class<T> clazz) {
        try {
            return (T) moduleClassMap.get(clazz);
        } catch (final Throwable e) {
            return null;
        }
    }

    public List<Module> getModules() {
        return moduleClassMap.values().stream().toList();
    }
}
