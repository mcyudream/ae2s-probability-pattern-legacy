package com.tz.statpatterns.verification;

import com.tz.statpatterns.StatPatternsMod;
import java.util.List;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import static com.tz.statpatterns.verification.RuntimeVerification.require;

/** This class is loaded reflectively only when the optional runtime is installed. */
public final class PonderVerification {
    public static void verify() throws Exception {
        var entries = PonderIndex.getSceneAccess().getRegisteredEntries().stream()
            .filter(e -> StatPatternsMod.NAMESPACE.equals(e.getValue().getNamespace())).toList();
        require(entries.size() == 3, "Expected three Ponder subject associations");
        var source = new PonderStructureLoader().load(StatPatternsMod.id("probability_processing"));
        require(source.getDiagnostics().isEmpty(), "Ponder has unresolved structure blocks: " + source.getDiagnostics());
        var scenes = PonderIndex.getSceneAccess().compile(List.of(entries.getFirst().getValue()));
        var scene = scenes.getFirst();
        for (String key : new String[] { "network", "encode", "confidence", "install", "request", "random", "wireless" }) {
            String text = PonderIndex.getLangAccess().getShared(StatPatternsMod.id(key));
            require(!text.startsWith("Format error:") && !text.contains("%%") && !text.equals("statpatterns.ponder.shared." + key),
                "Ponder narration failed to localize: " + key + ": " + text);
        }
        scene.seek(scene.getTotalTicks()); require(scene.isFinished(), "Full Ponder timeline failed");
        scene.seek(0); scene.seek(300);
        RuntimeVerification.INSTANCE.checked("Optional Ponder registers three subjects, loads structure, plays full timeline and rewinds: "
            + Minecraft.getMinecraft().gameSettings.language);
        Minecraft.getMinecraft().displayGuiScreen(new PonderUI(scenes));
        ((PonderUI) Minecraft.getMinecraft().currentScreen).seekToTime(300);
    }
}
