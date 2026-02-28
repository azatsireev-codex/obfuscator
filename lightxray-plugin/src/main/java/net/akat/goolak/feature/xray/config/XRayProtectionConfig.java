package net.akat.goolak.feature.xray.config;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public final class XRayProtectionConfig {

  private final boolean enabled;
  private final int minY;
  private final int maxY;
  private final double viewConeDegrees;
  private final int maxReplacementsPerChunk;
  private final Material replacementMaterial;
  private final Set<Material> hiddenMaterials;

  private XRayProtectionConfig(boolean enabled, int minY, int maxY, double viewConeDegrees,
      int maxReplacementsPerChunk, Material replacementMaterial, Set<Material> hiddenMaterials) {
    this.enabled = enabled;
    this.minY = minY;
    this.maxY = maxY;
    this.viewConeDegrees = viewConeDegrees;
    this.maxReplacementsPerChunk = maxReplacementsPerChunk;
    this.replacementMaterial = replacementMaterial;
    this.hiddenMaterials = hiddenMaterials;
  }

  public static XRayProtectionConfig from(FileConfiguration config) {
    boolean enabled = config.getBoolean("features.xray.enabled", true);

    int configuredMinY = config.getInt("features.xray.minY", -64);
    int configuredMaxY = config.getInt("features.xray.maxY", 64);
    int minY = Math.min(configuredMinY, configuredMaxY);
    int maxY = Math.max(configuredMinY, configuredMaxY);

    double viewConeDegrees = Math.min(180d, Math.max(1d,
        config.getDouble("features.xray.viewConeDegrees", 100d)));

    int maxReplacementsPerChunk = Math.max(1,
        config.getInt("features.xray.maxReplacementsPerChunk", 384));

    Material replacement = Material.matchMaterial(
        config.getString("features.xray.replacementMaterial", "STONE"));
    if (replacement == null || !replacement.isBlock()) {
      replacement = Material.STONE;
    }

    Set<Material> hiddenMaterials = EnumSet.noneOf(Material.class);
    List<String> values = config.getStringList("features.xray.hiddenMaterials");
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

    return new XRayProtectionConfig(enabled, minY, maxY, viewConeDegrees, maxReplacementsPerChunk,
        replacement, hiddenMaterials);
  }

  public boolean enabled() {
    return this.enabled;
  }

  public int minY() {
    return this.minY;
  }

  public int maxY() {
    return this.maxY;
  }

  public double viewConeDegrees() {
    return this.viewConeDegrees;
  }

  public int maxReplacementsPerChunk() {
    return this.maxReplacementsPerChunk;
  }

  public Material replacementMaterial() {
    return this.replacementMaterial;
  }

  public Set<Material> hiddenMaterials() {
    return this.hiddenMaterials;
  }
}
