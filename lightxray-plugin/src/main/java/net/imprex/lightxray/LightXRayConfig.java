package net.imprex.lightxray;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

final class LightXRayConfig {

  private final boolean enabled;
  private final int minY;
  private final int maxY;
  private final double viewConeDegrees;
  private final int maxReplacementsPerChunk;
  private final Material replacementMaterial;
  private final Set<Material> hiddenMaterials;

  private LightXRayConfig(boolean enabled, int minY, int maxY, double viewConeDegrees,
      int maxReplacementsPerChunk, Material replacementMaterial, Set<Material> hiddenMaterials) {
    this.enabled = enabled;
    this.minY = minY;
    this.maxY = maxY;
    this.viewConeDegrees = viewConeDegrees;
    this.maxReplacementsPerChunk = maxReplacementsPerChunk;
    this.replacementMaterial = replacementMaterial;
    this.hiddenMaterials = hiddenMaterials;
  }

  static LightXRayConfig from(FileConfiguration config) {
    boolean enabled = config.getBoolean("enabled", true);

    int configuredMinY = config.getInt("minY", -64);
    int configuredMaxY = config.getInt("maxY", 64);
    int minY = Math.min(configuredMinY, configuredMaxY);
    int maxY = Math.max(configuredMinY, configuredMaxY);

    double viewConeDegrees = Math.min(180d, Math.max(1d, config.getDouble("viewConeDegrees", 100d)));
    int maxReplacementsPerChunk = Math.max(1, config.getInt("maxReplacementsPerChunk", 384));

    Material replacement = Material.matchMaterial(config.getString("replacementMaterial", "STONE"));
    if (replacement == null || !replacement.isBlock()) {
      replacement = Material.STONE;
    }

    Set<Material> hiddenMaterials = EnumSet.noneOf(Material.class);
    List<String> values = config.getStringList("hiddenMaterials");
    for (String value : values) {
      Material material = Material.matchMaterial(value);
      if (material != null && material.isBlock()) {
        hiddenMaterials.add(material);
      }
    }

    if (hiddenMaterials.isEmpty()) {
      hiddenMaterials.add(Material.DIAMOND_ORE);
      hiddenMaterials.add(Material.DEEPSLATE_DIAMOND_ORE);
    }

    return new LightXRayConfig(enabled, minY, maxY, viewConeDegrees, maxReplacementsPerChunk,
        replacement, hiddenMaterials);
  }

  boolean enabled() {
    return this.enabled;
  }

  int minY() {
    return this.minY;
  }

  int maxY() {
    return this.maxY;
  }

  double viewConeDegrees() {
    return this.viewConeDegrees;
  }

  int maxReplacementsPerChunk() {
    return this.maxReplacementsPerChunk;
  }

  Material replacementMaterial() {
    return this.replacementMaterial;
  }

  Set<Material> hiddenMaterials() {
    return this.hiddenMaterials;
  }
}
