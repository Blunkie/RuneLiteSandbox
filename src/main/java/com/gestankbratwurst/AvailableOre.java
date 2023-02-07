package com.gestankbratwurst;

import com.gestankbratwurst.utils.Rock;
import lombok.Data;
import net.runelite.api.TileObject;

@Data
public class AvailableOre {
  private final Rock rock;
  private final TileObject tileObject;
}
