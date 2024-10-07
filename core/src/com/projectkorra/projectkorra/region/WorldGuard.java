package com.projectkorra.projectkorra.region;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.World;
import org.bukkit.entity.Player;

class WorldGuard extends RegionProtectionBase {

    protected WorldGuard() {
        super("WorldGuard");
    }

    @Override
    public boolean isRegionProtectedReal(Player player, org.bukkit.Location reallocation, CoreAbility ability, boolean igniteAbility, boolean explosiveAbility) {
        World world = reallocation.getWorld();

        if (player.hasPermission("worldguard.region.bypass." + world.getName())) return false;

        final com.sk89q.worldguard.WorldGuard wg = com.sk89q.worldguard.WorldGuard.getInstance();
        final Location location = BukkitAdapter.adapt(reallocation);
        if (igniteAbility) {
            if (!player.hasPermission("worldguard.override.lighter")) {
                if (wg.getPlatform().getGlobalStateManager().get(BukkitAdapter.adapt(world)).blockLighter) {
                    return true;
                }
            }
        }

        if (explosiveAbility) {
            if (wg.getPlatform().getGlobalStateManager().get(BukkitAdapter.adapt(world)).blockTNTExplosions) {
                return true;
            }
            final StateFlag.State tntflag = wg.getPlatform().getRegionContainer().createQuery().queryState(location, WorldGuardPlugin.inst().wrapPlayer(player), Flags.TNT);
            if (tntflag != null && tntflag.equals(StateFlag.State.DENY)) {
                return true;
            }
        }
        final StateFlag bendingflag = (StateFlag) com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry().get("bending");
        if (bendingflag != null) {
            final StateFlag.State bendingflagstate = wg.getPlatform().getRegionContainer().createQuery().queryState(location, WorldGuardPlugin.inst().wrapPlayer(player), bendingflag);
            boolean bendingDenied = bendingflagstate != null && bendingflagstate.equals(StateFlag.State.DENY);
            if (bendingDenied) {
                return true;
            }
        }
        RegionQuery query = wg.getPlatform().getRegionContainer().createQuery();
        LocalPlayer lPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        final StateFlag.State buildFlagState = query.queryState(location, lPlayer, Flags.BUILD);
        final StateFlag.State breakFlagState = query.queryState(location, lPlayer, Flags.BLOCK_BREAK);
        final StateFlag.State placeFlagState = query.queryState(location, lPlayer, Flags.BLOCK_PLACE);
        boolean buildDenied = buildFlagState != null && buildFlagState.equals(StateFlag.State.DENY);
        boolean breakDenied = breakFlagState != null && breakFlagState.equals(StateFlag.State.DENY);
        boolean placeDenied = placeFlagState != null && placeFlagState.equals(StateFlag.State.DENY);

        return buildDenied || breakDenied || placeDenied;
    }
}
