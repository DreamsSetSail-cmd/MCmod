package com.clion.echoesofoblivion.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PhantomEntity extends Monster {
    public PhantomEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        // 靠近玩家时缓慢吸取侵蚀值
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(3))) {
                int current = player.getPersistentData().getInt("echoes_corruption");
                player.getPersistentData().putInt("echoes_corruption", Math.min(100, current + 1));
            }
        }
    }
}