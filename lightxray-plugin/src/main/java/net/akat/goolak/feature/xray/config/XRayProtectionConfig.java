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
  private final boolean boundaryOnly;
  private final Material replacementMaterial;
  private final Set<Material> hiddenMaterials;

  private XRayProtectionConfig(boolean enabled, int minY, int maxY, boolean boundaryOnly,
      Material replacementMaterial, Set<Material> hiddenMaterials) {
    this.enabled = enabled;
    this.minY = minY;
    this.maxY = maxY;
    this.boundaryOnly = boundaryOnly;
    this.replacementMaterial = replacementMaterial;
    this.hiddenMaterials = hiddenMaterials;
  }

  public static XRayProtectionConfig from(FileConfiguration config) {
    boolean enabled = config.getBoolean("features.xray.enabled", true);

    int configuredMinY = config.getInt("features.xray.minY", -64);
    int configuredMaxY = config.getInt("features.xray.maxY", 64);
    int minY = Math.min(configuredMinY, configuredMaxY);
    int maxY = Math.max(configuredMinY, configuredMaxY);

    boolean boundaryOnly = config.getBoolean("features.xray.boundaryOnly", false);

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

    return new XRayProtectionConfig(enabled, minY, maxY, boundaryOnly, replacement, hiddenMaterials);
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

  public boolean boundaryOnly() {
    return this.boundaryOnly;
  }

  public Material replacementMaterial() {
    return this.replacementMaterial;
  }

  public Set<Material> hiddenMaterials() {
    return this.hiddenMaterials;
  }
}
