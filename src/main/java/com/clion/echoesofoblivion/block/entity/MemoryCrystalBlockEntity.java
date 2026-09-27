package com.clion.echoesofoblivion.block.entity;

import com.clion.echoesofoblivion.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MemoryCrystalBlockEntity extends BlockEntity {
    private boolean activated = false;
    private int activationTime = 0;

    public MemoryCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MEMORY_CRYSTAL_BE.get(), pos, state);
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        this.activationTime = (int) (System.currentTimeMillis() / 1000);
    }

    public int getActivationTime() {
        return activationTime;
    }
}