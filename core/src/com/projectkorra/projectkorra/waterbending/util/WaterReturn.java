package com.projectkorra.projectkorra.waterbending.util;

import java.util.HashMap;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.attribute.markers.DayNightFactor;
import com.projectkorra.projectkorra.versions.IBottleFinder;
import com.projectkorra.projectkorra.versions.legacy.LegacyBottleFinder;
import com.projectkorra.projectkorra.versions.modern.ModernBottleFinder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.ParticleEffect;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.OctopusForm;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import com.projectkorra.projectkorra.waterbending.ice.IceSpikeBlast;

public class WaterReturn extends WaterAbility {

	public static final IBottleFinder BOTTLE_FINDER = GeneralMethods.getMCVersion() >= 1204 ? new ModernBottleFinder() : new LegacyBottleFinder();

	private long time;
	private long interval;
	@Attribute(Attribute.RANGE) @DayNightFactor
	private double range;
	private Location location;
	private TempBlock block;

	public WaterReturn(final Player player, final Block block) {
		super(player);
		if (hasAbility(player, WaterReturn.class)) {
			return;
		}

		this.location = block.getLocation();
		this.range = 30;
		this.interval = 50;

		if (this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
			if (isTransparent(player, block) && ((TempBlock.isTempBlock(block) && block.isLiquid()) || !block.isLiquid()) && this.hasEmptyWaterBottle()) {
				this.block = new TempBlock(block, Material.WATER);
			}
		}
		this.start();
	}

	@Override
	public void progress() {
		if (!this.bPlayer.canBendIgnoreBindsCooldowns(this)) {
			this.remove();
			return;
		} else if (!this.hasEmptyWaterBottle() || bPlayer.hasWaterPouch()) {
			this.remove();
			return;
		} else if (System.currentTimeMillis() < this.time + this.interval) {
			return;
		}

		final Vector direction = GeneralMethods.getDirection(this.location, this.player.getEyeLocation()).normalize();
		this.time = System.currentTimeMillis();
		this.location = this.location.clone().add(direction);

		if (this.location == null || this.block == null) {
			this.remove();
			return;
		} else if (this.location.getBlock().equals(this.block.getLocation().getBlock())) {
			return;
		}

		if (this.location.distanceSquared(this.player.getEyeLocation()) > this.range * this.range) {
			this.remove();
			return;
		} else if (this.location.distanceSquared(this.player.getEyeLocation()) <= 1.5 * 1.5) {
			this.fillBottle();
			return;
		}

		final Block newblock = this.location.getBlock();
		if (isTransparent(this.player, newblock) && !newblock.isLiquid()) {
			this.block.revertBlock();
			this.block = new TempBlock(newblock, Material.WATER);
		} else if (isTransparent(this.player, newblock)) {
			if (isWater(newblock)) {
				ParticleEffect.WATER_BUBBLE.display(newblock.getLocation().clone().add(.5, .5, .5), 5, Math.random(), Math.random(), Math.random(), 0);
			}
		} else {
			this.remove();
			return;
		}

	}

	@Override
	public void remove() {
		super.remove();
		if (this.block != null) {
			this.block.revertBlock();
		}
	}

	private boolean hasEmptyWaterBottle() {
		final PlayerInventory inventory = this.player.getInventory();
		if (inventory.contains(Material.GLASS_BOTTLE)) {
			return true;
		}
		return false;
	}

	private void fillBottle() {
		final PlayerInventory inventory = this.player.getInventory();
		final int index = inventory.first(Material.GLASS_BOTTLE);
		if (index >= 0) {
			final ItemStack item = inventory.getItem(index);

			final ItemStack water = waterBottleItem();

			if (item.getAmount() == 1) {
				inventory.setItem(index, water);
			} else {
				item.setAmount(item.getAmount() - 1);
				inventory.setItem(index, item);
				final HashMap<Integer, ItemStack> leftover = inventory.addItem(water);
				for (final int left : leftover.keySet()) {
					this.player.getWorld().dropItemNaturally(this.player.getLocation(), leftover.get(left));
				}
			}
		}
		this.remove();
	}

	private static boolean isBending(final Player player) {
		if (hasAbility(player, WaterManipulation.class) || hasAbility(player, WaterManipulation.class) || hasAbility(player, OctopusForm.class)
		// || hasAbility(player, SurgeWave.class) NOTE: ONLY DISABLED TO
		// PREVENT BOTTLEBENDING FROM BEING DISABLED FOREVER. ONCE
		// BOTTLEBENDING HAS BEEN RECODED IN 1.9, THIS NEEDS TO BE
		// READDED TO THE NEW SYSTEM.
				|| hasAbility(player, SurgeWall.class) || hasAbility(player, IceSpikeBlast.class)) {
			return true;
		}
		return false;
	}

	public static int firstWaterBottle(final PlayerInventory inventory) {
		return BOTTLE_FINDER.findWaterBottle(inventory);
	}

	public static boolean hasWaterBottle(final Player player) {
		if (hasAbility(player, WaterReturn.class) || isBending(player)) {
			return false;
		}
		if (BendingPlayer.getBendingPlayer(player).hasWaterPouch())
			return true;
		final PlayerInventory inventory = player.getInventory();

		return WaterReturn.firstWaterBottle(inventory) >= 0;
	}

	public static void emptyWaterBottle(final Player player) {
		final PlayerInventory inventory = player.getInventory();
		int index = WaterReturn.firstWaterBottle(inventory);

		if (index != -1) {
			final ItemStack item = inventory.getItem(index);
			if (item.getAmount() == 1) {
				inventory.setItem(index, new ItemStack(Material.GLASS_BOTTLE));
			} else {
				item.setAmount(item.getAmount() - 1);
				inventory.setItem(index, item);
				final HashMap<Integer, ItemStack> leftover = inventory.addItem(new ItemStack(Material.GLASS_BOTTLE));

				for (final int left : leftover.keySet()) {
					player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(left));
				}
			}
		}
	}

	public static ItemStack waterBottleItem() {
		return BOTTLE_FINDER.createWaterBottle();
	}

	public long getTime() {
		return this.time;
	}

	public void setTime(final long time) {
		this.time = time;
	}

	public long getInterval() {
		return this.interval;
	}

	public void setInterval(final long interval) {
		this.interval = interval;
	}

	public double getRange() {
		return this.range;
	}

	public void setRange(final double range) {
		this.range = range;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public boolean isSneakAbility() {
		return false;
	}

	@Override
	public boolean isHarmlessAbility() {
		return true;
	}

	public void setLocation(final Location location) {
		this.location = location;
	}

	public TempBlock getBlock() {
		return this.block;
	}

	public void setBlock(final TempBlock block) {
		this.block = block;
	}

	@Override
	public String getName() {
		return "Bottlebending";
	}

	@Override
	public long getCooldown() {
		return 0;
	}

	@Override
	public boolean isHiddenAbility() {
		return true;
	}

}
