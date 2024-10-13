package com.example.modid;

import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockLog;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION)
public class ExampleMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Hello From {}!", Tags.MOD_NAME);
        // Register the event handler with the event bus
        MinecraftForge.EVENT_BUS.register(new PlayerMoveEventHandler());
    }

    public static class PlayerMoveEventHandler {

        private static final Random random = new Random(); // Random instance
        private static final double TREE_BREAK_CHANCE = 0.05; // 5% chance
        private static final double SIGN_PLACEMENT_CHANCE = 0.03; // 3% chance

        @SubscribeEvent
        public void onPlayerMove(LivingEvent.LivingUpdateEvent event) {
            if (event.getEntityLiving() instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) event.getEntityLiving();
                World world = player.getEntityWorld();

                // Detecting the player's bed location as their base
                BlockPos basePos = player.getBedLocation(player.dimension);
                if (basePos != null && basePos.distanceSq(player.getPosition()) < 100) {
                    // Randomly decide whether to place a sign or break a tree
                    if (random.nextDouble() < SIGN_PLACEMENT_CHANCE) {
                        placeSignOutsideBase(player, world, basePos); // 3% chance to place a sign
                    }
                    if (random.nextDouble() < TREE_BREAK_CHANCE) {
                        breakRandomTreeNearBase(player, world, basePos); // 5% chance to break a tree
                    }
                    BlockPos doorPos = findDoor(world, basePos);
                    if (doorPos != null && random.nextDouble() < 0.05) { // 5% chance
                        if (!isPlayerLookingAtBlock(player, doorPos, world)) {
                            boolean isOpen = world.getBlockState(doorPos).getValue(BlockDoor.OPEN);

                            // Play the appropriate sound based on whether the door is opening or closing
                            if (!isOpen) {
                                // Door is currently closed, so it will open
                                world.playSound(null, doorPos, SoundEvents.BLOCK_WOODEN_DOOR_OPEN,
                                        net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
                            } else {
                                // Door is currently open, so it will close
                                world.playSound(null, doorPos, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                                        net.minecraft.util.SoundCategory.BLOCKS, 1.0F, 1.0F);
                            }

                            // Toggle door state
                            world.setBlockState(doorPos, world.getBlockState(doorPos).withProperty(BlockDoor.OPEN, !isOpen));
                        }
                    }

                }
            }
        }

        // Method to place a sign outside the player's base (same as before)
        public void placeSignOutsideBase(EntityPlayer player, World world, BlockPos basePos) {
            BlockPos doorPos = findDoor(world, basePos);

            if (doorPos != null) {
                BlockPos signPos = getSignPosition(world, doorPos);

                if (!isPlayerLookingAtBlock(player, signPos, world)) {
                    if (world.isAirBlock(signPos) && world.getBlockState(signPos.down()).isFullBlock()) {
                        world.setBlockState(signPos, Blocks.STANDING_SIGN.getDefaultState());

                        TileEntitySign signTile = (TileEntitySign) world.getTileEntity(signPos);
                        if (signTile != null) {
                            signTile.signText[0] = new TextComponentString("Leave me alone");
                            signTile.markDirty();
                        }
                    }
                }
            }
        }

        // Method to randomly break a tree near the base (same as before)
        public void breakRandomTreeNearBase(EntityPlayer player, World world, BlockPos basePos) {
            int searchRadius = 100; // Search for a tree within this radius

            // Iterate over blocks within the radius to find a tree (log block)
            for (BlockPos pos : BlockPos.getAllInBox(basePos.add(-searchRadius, 0, -searchRadius), basePos.add(searchRadius, 10, searchRadius))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockLog) {
                    LOGGER.info("Breaking a random tree at " + pos);
                    fellTree(world, pos); // Break the entire tree
                    break; // Break only one tree at a time
                }
            }
        }

        // Helper method to find the door of the player's base (same as before)
        private BlockPos findDoor(World world, BlockPos basePos) {
            int searchRadius = 10;
            for (BlockPos pos : BlockPos.getAllInBox(basePos.add(-searchRadius, 0, -searchRadius), basePos.add(searchRadius, 2, searchRadius))) {
                if (world.getBlockState(pos).getBlock() instanceof BlockDoor) {
                    return pos;
                }
            }
            return null;
        }

        // Helper method to determine an appropriate position for the sign (same as before)
        private BlockPos getSignPosition(World world, BlockPos doorPos) {
            EnumFacing facing = world.getBlockState(doorPos).getValue(BlockDoor.FACING);
            return doorPos.offset(facing.getOpposite(), 2);
        }

        // Helper method to check if the player is looking at the block (same as before)
        private boolean isPlayerLookingAtBlock(EntityPlayer player, BlockPos blockPos, World world) {
            // Get the player's eye position and the block's position
            Vec3d playerEyes = player.getPositionEyes(1.0F); // Get the player's eye position with partial ticks
            Vec3d blockCenter = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5); // Center of the block

            // Define the maximum distance the player can look at (e.g., 10 blocks)
            double maxDistance = 10.0;

            // Perform a ray trace from the player's eyes to the block
            RayTraceResult result = world.rayTraceBlocks(playerEyes, blockCenter, false, true, false);

            // Check if the result hit the target block
            if (result != null && result.getBlockPos().equals(blockPos)) {
                return true; // Player has clear line of sight to the block
            }

            return false; // Player is not looking at the block
        }

        // Method to fell the tree (same as before)
        private void fellTree(World world, BlockPos basePos) {
            Queue<BlockPos> logsToBreak = new LinkedList<>();
            Set<BlockPos> visited = new HashSet<>();

            // Add the initial position (base of the tree) to the queue
            logsToBreak.add(basePos);

            // Process the queue until it's empty
            while (!logsToBreak.isEmpty()) {
                BlockPos currentPos = logsToBreak.poll();

                // Skip if this block has already been visited
                if (visited.contains(currentPos)) {
                    continue;
                }

                // Mark this block as visited
                visited.add(currentPos);

                // Check if the current block is a log
                if (world.getBlockState(currentPos).getBlock() instanceof BlockLog) {
                    // Destroy the block
                    world.destroyBlock(currentPos, true); // Drop the log as an item

                    // Add neighboring blocks to the queue for further checks (in all directions)
                    for (EnumFacing direction : EnumFacing.values()) {
                        BlockPos neighborPos = currentPos.offset(direction);
                        if (!visited.contains(neighborPos)) {
                            logsToBreak.add(neighborPos);
                        }
                    }
                }
            }
        }
    }
}
