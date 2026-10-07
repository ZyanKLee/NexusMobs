package com.nexusmobs.loot;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

/**
 * Finds where the TELEPORT item ability may put a player (#9).
 *
 * <p>Walks along the player's line of sight and keeps the last position where the player fits:
 * feet and head block neither solid nor liquid. It stops at the first solid block, so the player
 * ends up in front of a wall or on top of the floor they look at, never inside a block.
 */
public final class TeleportTargets {

    private static final double STEP = 0.25;

    private TeleportTargets() {
    }

    /**
     * @param eye         the player's eye location (its direction is the line of sight)
     * @param maxDistance how far the player may teleport
     * @return the target feet location (block centre, keeping yaw/pitch), or null if there is no room
     */
    public static Location find(Location eye, double maxDistance) {
        Vector direction = eye.getDirection().normalize();
        Location target = null;

        for (double distance = STEP; distance <= maxDistance; distance += STEP) {
            Block block = eye.clone().add(direction.clone().multiply(distance)).getBlock();
            if (!isFree(block)) {
                break; // line of sight hits something
            }
            Block feet = standingSpot(block);
            if (feet != null && !isStartBlock(feet, eye)) {
                // built from coordinates: never mutate a Location a Block might hand out
                target = new Location(eye.getWorld(), feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
            }
        }

        if (target != null) {
            target.setYaw(eye.getYaw());
            target.setPitch(eye.getPitch());
        }
        return target;
    }

    /** The block the player is standing in: teleporting there would only waste the cooldown. */
    private static boolean isStartBlock(Block feet, Location eye) {
        return feet.getX() == eye.getBlockX() && feet.getZ() == eye.getBlockZ()
                && Math.abs(feet.getY() - eye.getBlockY()) <= 1;
    }

    /** The feet block for a free block on the line of sight, or null if a player does not fit. */
    private static Block standingSpot(Block onRay) {
        Block below = onRay.getRelative(BlockFace.DOWN);
        if (isFree(below)) {
            return below; // ray at head height
        }
        if (isFree(onRay.getRelative(BlockFace.UP))) {
            return onRay; // ray at feet height (e.g. just above the floor)
        }
        return null;
    }

    /** No collision and not a liquid. Same solidity rule as the spawner's safe-location check. */
    private static boolean isFree(Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }
}
