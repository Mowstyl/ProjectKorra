package com.projectkorra.projectkorra.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.EarthAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;

import io.papermc.lib.PaperLib;

public class RevertChecker implements Runnable {
	private static final FileConfiguration CONFIG = ConfigManager.defaultConfig.get(); // TODO: Remove this and allow for values to be updated when reloaded
	private static final boolean SAFE_REVERT = CONFIG.getBoolean("Properties.Earth.SafeRevert");
	private static final Map<Integer, Integer> AIR_REVERT_QUEUE = new ConcurrentHashMap<>();
	private static final Map<Block, Block> EARTH_REVERT_QUEUE = new ConcurrentHashMap<>();

	private final ProjectKorra plugin;

    public RevertChecker(final ProjectKorra bending) {
		this.plugin = bending;
	}

	public static void revertAirBlocks() {
		for (final int id : AIR_REVERT_QUEUE.keySet()) {
			PaperLib.getChunkAtAsync(EarthAbility.getTempAirLocations().get(id).getState().getBlock().getLocation()).thenAccept(result -> EarthAbility.revertAirBlock(id));
			RevertChecker.AIR_REVERT_QUEUE.remove(id);
		}
	}

	public static void revertEarthBlocks() {
		for (final Block block : EARTH_REVERT_QUEUE.keySet()) {
			PaperLib.getChunkAtAsync(block.getLocation()).thenAccept(result -> EarthAbility.revertBlock(block));
			EARTH_REVERT_QUEUE.remove(block);
		}
	}

    private void addToAirRevertQueue(final int i) {
		AIR_REVERT_QUEUE.putIfAbsent(i, i);
	}

	private void addToRevertQueue(final Block block) {
		if (!EARTH_REVERT_QUEUE.containsKey(block)) {
			EARTH_REVERT_QUEUE.put(block, block);
		}
	}

	@Override
	public void run() {
		if (!this.plugin.isEnabled()) {
			return;
		}

        long time = System.currentTimeMillis();
		if (CONFIG.getBoolean("Properties.Earth.RevertEarthbending")) {

			try {
                Future<Set<Long>> returnFuture = this.plugin.getServer().getScheduler().callSyncMethod(this.plugin, () -> {
					final Set<Long> chunks = new HashSet<>();
					for (final Player player : Bukkit.getOnlinePlayers()) {
						Location location = player.getLocation();
						chunks.add(packChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4));
					}
					return chunks;
				});

				final Set<Long> chunks = returnFuture.get();

				final Map<Block, Information> earth = new HashMap<>(EarthAbility.getMovedEarth());

				for (final Block block : earth.keySet()) {
					if (EARTH_REVERT_QUEUE.containsKey(block)) {
						continue;
					}

					final Information info = earth.get(block);
					final long chunk = packChunk(block.getX() >> 4, block.getZ() >> 4);

					if (time > (info.getTime() + CONFIG.getLong("Properties.Earth.RevertCheckTime")) && !(chunks.contains(chunk) && SAFE_REVERT)) {
						this.addToRevertQueue(block);
					}
				}

				final Map<Integer, Information> air = new HashMap<>(EarthAbility.getTempAirLocations());

				for (final Integer i : air.keySet()) {
					if (AIR_REVERT_QUEUE.containsKey(i)) {
						continue;
					}

					final Information info = air.get(i);
					final Block block = info.getBlock();
					final long chunk = packChunk(block.getX() >> 4, block.getZ() >> 4);

					if (time > (info.getTime() + CONFIG.getLong("Properties.Earth.RevertCheckTime")) && !(chunks.contains(chunk) && SAFE_REVERT)) {
						this.addToAirRevertQueue(i);
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static long packChunk(int x, int z) {
		return (((long) x) << 32) | (z & 0xFFFFFFFFL);
	}

}
