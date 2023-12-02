package com.zenith.cache.data.config;

import net.kyori.adventure.text.Component;

import java.util.UUID;

public record ResourcePack(UUID id, String url, String hash, boolean required, Component prompt) { }
