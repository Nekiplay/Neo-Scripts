package com.nekiplay.hypixelcry.utils.trackers;

import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.utils.Utils;
import com.nekiplay.hypixelcry.utils.scheduler.Scheduler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColdTracker {
    private static final Pattern COLD_PATTERN = Pattern.compile("Cold: -(\\d+)❄");
    private static int cold = 0;
    private static long resetTime = System.currentTimeMillis();

    @Init
    public static void init() {
        Scheduler.INSTANCE.scheduleCyclic(ColdTracker::update, 20);
        ClientReceiveMessageEvents.ALLOW_GAME.register(ColdTracker::coldReset);
    }

    private static boolean coldReset(Component text, boolean b) {
        if (!Utils.isInDwarvenMines() || b) {
            return true;
        }
        String message = text.getString();
        if (message.equals("The warmth of the campfire reduced your ❄ Cold to 0!")) {
            cold = 0;
            resetTime = System.currentTimeMillis();
        }

        return true;
    }

    private static void update() {
        if (!Utils.isInDwarvenMines() || System.currentTimeMillis() - resetTime < 3000) {
            cold = 0;
            return;
        }
        for (String line : Utils.STRING_SCOREBOARD) {
            Matcher coldMatcher = COLD_PATTERN.matcher(line);
            if (coldMatcher.matches()) {
                String value = coldMatcher.group(1);
                cold = Integer.parseInt(value);
                return;
            }
        }
        cold = 0;
    }

    public static int getCold() {
        return cold;
    }
}
