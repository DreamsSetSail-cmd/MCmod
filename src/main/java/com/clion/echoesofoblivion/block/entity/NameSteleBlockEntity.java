package com.clion.echoesofoblivion.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 名字碑的方块实体（v2.3.0）——**存放「谁把自己写了上去」**。
 *
 * <h2>为什么存了却永远不显示</h2>
 *
 * <p>这是这一版最重要的一个决定。碑上刻了名字，但**任何渲染路径都不会把它显示出来**：
 * 玩家看不到自己的名字，别人也看不到，客户端甚至收不到那个字符串。
 *
 * <p>理由是设定。这个文明的名字**被替换掉了**——`docs/story-bible.md` 里
 * 名字碑必须空白，因为「用写满名字的碑去纪念一个连名字都被替换掉的文明」
 * 是没有意义的。那么让玩家把自己写上去，如果还给他看见，
 * 就变成了一个「我留下了痕迹」的满足感，而这一版要的是相反的：
 *
 * <blockquote>你把自己写进了那个文明的位置。这不是胜利，是接替。</blockquote>
 *
 * <p>代价是真实的（侵蚀增长加快、开始出现关于你的记录式短句），
 * 收益是**零可见回报**。玩家会自己意识到这一点——那比任何提示都有效。
 *
 * <h2>那为什么还要存</h2>
 *
 * <p>因为「刻上去了」这件事必须是真的。它要能：
 * <ul>
 *   <li>持久化（重登后仍然算数）；</li>
 *   <li>被服务端查询（决定是否施加代价）；</li>
 *   <li>与具体的碑绑定（同一玩家可以刻多块，每块都记录自己的时间）。</li>
 * </ul>
 *
 * <p>所以它是一个真实的、存进存档的状态——只是**不进渲染**。
 */
public class NameSteleBlockEntity extends BlockEntity {

    /** 刻名者的 UUID。未刻名时为 null。 */
    private UUID inscriber;

    /** 刻名者的显示名。仅用于服务端日志与调试，不发给客户端。 */
    private String inscriberName = "";

    /** 刻名的游戏刻。 */
    private long inscribedAt;

    public NameSteleBlockEntity(BlockPos pos, BlockState state) {
        super(com.clion.echoesofoblivion.block.ModBlocks.NAME_STELE_BE.get(), pos, state);
    }

    public boolean isInscribed() {
        return inscriber != null;
    }

    public UUID inscriber() {
        return inscriber;
    }

    public String inscriberName() {
        return inscriberName;
    }

    public long inscribedAt() {
        return inscribedAt;
    }

    /** 刻名。**只能成功一次**——碑不接受改写。 */
    public boolean inscribe(UUID playerId, String playerName, long gameTime) {
        if (isInscribed()) {
            return false;
        }
        this.inscriber = playerId;
        this.inscriberName = playerName == null ? "" : playerName;
        this.inscribedAt = gameTime;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (inscriber != null) {
            tag.putUUID("inscriber", inscriber);
            tag.putString("inscriber_name", inscriberName);
            tag.putLong("inscribed_at", inscribedAt);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("inscriber")) {
            this.inscriber = tag.getUUID("inscriber");
            this.inscriberName = tag.getString("inscriber_name");
            this.inscribedAt = tag.getLong("inscribed_at");
        }
    }
}
