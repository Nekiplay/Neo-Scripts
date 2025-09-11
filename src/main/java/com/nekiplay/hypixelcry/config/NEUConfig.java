package com.nekiplay.hypixelcry.config;

import com.google.gson.annotations.Expose;
import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.config.neupages.ESP;
import com.nekiplay.hypixelcry.config.neupages.Macros;
import com.nekiplay.hypixelcry.config.neupages.Misc;
import com.nekiplay.hypixelcry.utils.Utils;
import io.github.notenoughupdates.moulconfig.Config;
import io.github.notenoughupdates.moulconfig.Social;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.common.MyResourceLocation;
import io.github.notenoughupdates.moulconfig.common.text.StructuredText;

import java.util.Arrays;
import java.util.List;

public class NEUConfig extends Config {
    @Override
    public StructuredText getTitle() {
        return StructuredText.of("§6Hypixel Cry §7v" + "1.1.2");
    }

    @Override
    public void saveNow() {
        HypixelCry.saveConfig();
    }

    private Social social(String name, String iconName, String link) {
        return new Social() {
            @Override
            public void onClick() {
                Utils.openUrl(link);
            }

            @Override
            public List<StructuredText> getTooltip() {
                return List.of(StructuredText.of("§7Open " + link));
            }

            @Override
            public MyResourceLocation getIcon() {
                return new MyResourceLocation("hypixelcry", "social/" + iconName + ".png");
            }
        };
    }

    @Override
    public List<Social> getSocials() {
        return Arrays.asList(
                social(
                        "YouTube",
                        "youtube",
                        "https://www.youtube.com/@Nekiplay"
                ),
                social(
                        "GitHub",
                        "github",
                        "https://github.com/Nekiplay/Hypixel-Cry"
                )
        );
    }

    @Category(
            name = "Macros",
            desc = "Auto and semi-auto features"
    )
    @Expose
    public Macros macros = new Macros();

    @Category(
            name = "ESP",
            desc = "Visual features"
    )
    @Expose
    public ESP esp = new ESP();

    @Category(
            name = "Misc",
            desc = "Other features"
    )
    @Expose
    public Misc misc = new Misc();

}
