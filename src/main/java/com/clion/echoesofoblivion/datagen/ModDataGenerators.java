package com.clion.echoesofoblivion.datagen;

import com.clion.echoesofoblivion.EchoesOfOblivionMod;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 数据生成入口（阶段 7）。
 *
 * <p>通过 {@code gradlew runData} 触发：ForgeGradle 会带上
 * {@code --mod echoesofoblivion --all --output src/generated/resources --existing src/main/resources}
 * 参数（见 {@code build.gradle} 的 {@code data} run 配置），
 * 生成结果直接落到 {@code src/generated/resources}，并被
 * {@code sourceSets.main.resources { srcDir 'src/generated/resources' }} 纳入构建。
 *
 * <p>{@code --existing src/main/resources} 让 {@link ExistingFileHelper} 能找到手写资源，
 * 因此 provider 可以校验引用的贴图确实存在——这正是最有用的一层保护：
 * 贴图改名而模型没跟着改时，datagen 会直接报错，而不是进游戏变成紫黑格。
 */
@Mod.EventBusSubscriber(modid = EchoesOfOblivionMod.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {

    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();

        if (event.includeClient()) {
            generator.addProvider(true, new ModBlockStateProvider(output, helper));
            generator.addProvider(true, new ModItemModelProvider(output, helper));
        }
    }
}
