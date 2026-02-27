package net.imprex.lightxray;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

final class LightXRayConfig {

  private final boolean enabled;
  private final int scanIntervalTicks;
  private final int chunkRadius;
  private final int minY;
  private final int maxY;
  private final double viewConeDegrees;
  private final int maxReplacementsPerScan;
  private final int maxRestoresPerScan;
  private final Material replacementMaterial;
  private final Set<Material> hiddenMaterials;

  private LightXRayConfig(boolean enabled, int scanIntervalTicks, int chunkRadius, int minY, int maxY,
      double viewConeDegrees, int maxReplacementsPerScan, int maxRestoresPerScan,
      Material replacementMaterial, Set<Material> hiddenMaterials) {
    this.enabled = enabled;
    this.scanIntervalTicks = scanIntervalTicks;
    this.chunkRadius = chunkRadius;
    this.minY = minY;
    this.maxY = maxY;
    this.viewConeDegrees = viewConeDegrees;
    this.maxReplacementsPerScan = maxReplacementsPerScan;
    this.maxRestoresPerScan = maxRestoresPerScan;
    this.replacementMaterial = replacementMaterial;
    this.hiddenMaterials = hiddenMaterials;
  }

  static LightXRayConfig from(FileConfiguration config) {
    boolean enabled = config.getBoolean("enabled", true);
    int scanIntervalTicks = Math.max(1, config.getInt("scanIntervalTicks", 20));
    int chunkRadius = Math.max(0, config.getInt("chunkRadius", 1));

    int configuredMinY = config.getInt("minY", -64);
    int configuredMaxY = config.getInt("maxY", 64);
    int minY = Math.min(configuredMinY, configuredMaxY);
    int maxY = Math.max(configuredMinY, configuredMaxY);

    double viewConeDegrees = Math.min(180d, Math.max(1d, config.getDouble("viewConeDegrees", 100d)));
    int maxReplacementsPerScan = Math.max(1, config.getInt("maxReplacementsPerScan", 800));
    int maxRestoresPerScan = Math.max(1, config.getInt("maxRestoresPerScan", 400));

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

    return new LightXRayConfig(enabled, scanIntervalTicks, chunkRadius, minY, maxY, viewConeDegrees,
        maxReplacementsPerScan, maxRestoresPerScan, replacement, hiddenMaterials);
  }

  boolean enabled() {
    return this.enabled;
  }

  int scanIntervalTicks() {
    return this.scanIntervalTicks;
  }

  int chunkRadius() {
    return this.chunkRadius;
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

  int maxReplacementsPerScan() {
    return this.maxReplacementsPerScan;
  }

  int maxRestoresPerScan() {
    return this.maxRestoresPerScan;
  }

  Material replacementMaterial() {
    return this.replacementMaterial;
  }

  Set<Material> hiddenMaterials() {
    return this.hiddenMaterials;
  }
}
