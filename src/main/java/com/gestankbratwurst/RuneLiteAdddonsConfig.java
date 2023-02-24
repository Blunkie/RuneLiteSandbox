package com.gestankbratwurst;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("OreBot")
public interface RuneLiteAdddonsConfig extends Config {
  @ConfigItem(
          keyName = "max-ore-distance",
          name = "Maximale Erz Reichweite",
          description = "Wie weit entfernt ein Erz sein darf, um drauf zu klicken."
  )
  default double maxOreDistance() {
    return 3.0;
  }
  @ConfigItem(
          keyName = "max-tree-distance",
          name = "Maximale Baum Reichweite",
          description = "Wie weit entfernt ein Baum sein darf, um drauf zu klicken."
  )
  default double maxTreeDistance() {
    return 16.0;
  }
  @ConfigItem(
          keyName = "pickup-item-on-fight",
          name = "Items beim Kämpfen aufheben",
          description = "Ob beim autokampf items aufgehoben werden."
  )
  default boolean pickupItemsOnFight() {
    return true;
  }

  @ConfigItem(
          keyName = "max-idle-for-retry",
          name = "Mining: Max idle für retry",
          description = "Wie lange in millis gewartet wird, bis ein neuer mining versuch gestartet wird."
  )
  default int maxIdleForRetryMining() {
    return 3000;
  }

  @ConfigItem(
          keyName = "use-fast-clicks",
          name = "Use fast mouse",
          description = "Benutzt eine etwas schnellere Maus."
  )
  default boolean useFastMouse() {
    return false;
  }
}
