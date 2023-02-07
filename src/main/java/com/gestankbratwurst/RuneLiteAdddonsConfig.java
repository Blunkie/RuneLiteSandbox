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
    return 2.0;
  }
  @ConfigItem(
          keyName = "pickup-item-on-fight",
          name = "Items beim Kämpfen aufheben",
          description = "Ob beim autokampf items aufgehoben werden."
  )
  default boolean pickupItemsOnFight() {
    return true;
  }
}
