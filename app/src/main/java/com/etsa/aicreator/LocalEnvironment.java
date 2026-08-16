package com.etsa.aicreator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

/**
 * Rebuilds a deterministic, batched detail field around the current camera focus.
 * This is deliberately local so the same interface can later be backed by streamed chunks.
 */
final class LocalEnvironment {
    private static final float DETAIL_RADIUS = 760f;
    private static final float REBUILD_DISTANCE = 150f;
    private static final float CELL_SIZE = 34f;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;

    private final ModelCache cache = new ModelCache();
    private final Vector3 lastCenter = new Vector3(Float.MAX_VALUE, 0f, Float.MAX_VALUE);

    private final Model trunkModel;
    private final Model paleTrunkModel;
    private final Model coniferCrownModel;
    private final Model broadleafCrownModel;
    private final Model fruitModel;
    private final Model bushModel;
    private final Model rockModel;
    private final Model tallRockModel;
    private final Model grassModel;

    LocalEnvironment() {
        ModelBuilder builder = new ModelBuilder();
        trunkModel = builder.createCylinder(1.7f, 9f, 1.7f, 7,
                material(0.28f, 0.19f, 0.11f), ATTRIBUTES);
        paleTrunkModel = builder.createCylinder(1.55f, 9f, 1.55f, 7,
                material(0.56f, 0.52f, 0.43f), ATTRIBUTES);
        coniferCrownModel = builder.createCone(8.5f, 18f, 8.5f, 9,
                material(0.10f, 0.27f, 0.13f), ATTRIBUTES);
        broadleafCrownModel = builder.createSphere(10f, 9f, 10f, 8, 6,
                material(0.16f, 0.38f, 0.16f), ATTRIBUTES);
        fruitModel = builder.createSphere(1.25f, 1.25f, 1.25f, 6, 4,
                material(0.70f, 0.16f, 0.08f), ATTRIBUTES);
        bushModel = builder.createSphere(4.5f, 2.8f, 4.5f, 7, 5,
                material(0.20f, 0.36f, 0.14f), ATTRIBUTES);
        rockModel = builder.createSphere(4f, 3f, 3.5f, 7, 5,
                material(0.38f, 0.38f, 0.36f), ATTRIBUTES);
        tallRockModel = builder.createSphere(3.2f, 5.2f, 3f, 7, 5,
                material(0.34f, 0.35f, 0.34f), ATTRIBUTES);
        grassModel = builder.createCone(2.4f, 3.6f, 2.4f, 5,
                material(0.30f, 0.46f, 0.17f), ATTRIBUTES);
    }

    void update(PerspectiveCamera camera, Vector3 focus) {
        float dx = focus.x - lastCenter.x;
        float dz = focus.z - lastCenter.z;
        if (dx * dx + dz * dz < REBUILD_DISTANCE * REBUILD_DISTANCE) {
            return;
        }

        lastCenter.set(focus);
        cache.begin(camera);

        int minCellX = (int) Math.floor((focus.x - DETAIL_RADIUS) / CELL_SIZE);
        int maxCellX = (int) Math.ceil((focus.x + DETAIL_RADIUS) / CELL_SIZE);
        int minCellZ = (int) Math.floor((focus.z - DETAIL_RADIUS) / CELL_SIZE);
        int maxCellZ = (int) Math.ceil((focus.z + DETAIL_RADIUS) / CELL_SIZE);

        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                float x = (cellX + WorldGenerator.random01(cellX, cellZ, 3) * 0.82f) * CELL_SIZE;
                float z = (cellZ + WorldGenerator.random01(cellX, cellZ, 5) * 0.82f) * CELL_SIZE;
                float offsetX = x - focus.x;
                float offsetZ = z - focus.z;
                float distanceSquared = offsetX * offsetX + offsetZ * offsetZ;
                if (distanceSquared > DETAIL_RADIUS * DETAIL_RADIUS) {
                    continue;
                }

                float distance = (float) Math.sqrt(distanceSquared);
                if (!EtsaWorld.containsTerrainPosition(x, z)) {
                    continue;
                }
                float height = EtsaWorld.terrainSurfaceHeight(x, z);
                float slope = WorldGenerator.slope(x, z);
                if (height <= WorldGenerator.WATER_LEVEL + 1f || slope > 0.92f) {
                    continue;
                }

                float nearDensity = distance < 285f ? 1f : distance < 510f ? 0.52f : 0.20f;
                float forest = WorldGenerator.forestDensity(x, z);
                float selection = WorldGenerator.random01(cellX, cellZ, 11);

                if (selection < forest * nearDensity * 0.92f) {
                    addTree(cellX, cellZ, x, height, z, distance);
                } else if (distance < 500f
                        && selection < forest * nearDensity + 0.16f * nearDensity) {
                    addBush(cellX, cellZ, x, height, z);
                }

                float rockChance = 0.035f + slope * 0.22f;
                if (WorldGenerator.random01(cellX, cellZ, 17) < rockChance * nearDensity) {
                    addRock(cellX, cellZ, x, height, z);
                }

                if (distance < 310f
                        && slope < 0.34f
                        && WorldGenerator.random01(cellX, cellZ, 23) < 0.48f) {
                    addGrass(cellX, cellZ, x, height, z);
                }
            }
        }

        cache.end();
    }

    ModelCache cache() {
        return cache;
    }

    void dispose() {
        cache.dispose();
        trunkModel.dispose();
        paleTrunkModel.dispose();
        coniferCrownModel.dispose();
        broadleafCrownModel.dispose();
        fruitModel.dispose();
        bushModel.dispose();
        rockModel.dispose();
        tallRockModel.dispose();
        grassModel.dispose();
    }

    private void addTree(int cellX, int cellZ, float x, float y, float z, float distance) {
        float random = WorldGenerator.random01(cellX, cellZ, 31);
        float scale = 0.72f + WorldGenerator.random01(cellX, cellZ, 37) * 0.62f;
        if (distance > 510f) {
            scale *= 0.88f;
        }
        float angle = WorldGenerator.random01(cellX, cellZ, 41) * 360f;
        float trunkHeight = 9f * scale;
        boolean conifer = y > 58f || random < 0.46f;
        boolean fruitTree = !conifer && random > 0.84f && y < 52f && distance < 430f;
        Model selectedTrunk = !conifer && random > 0.70f ? paleTrunkModel : trunkModel;
        add(selectedTrunk, x, y + trunkHeight * 0.5f, z, angle, scale, scale, scale);

        if (conifer) {
            add(coniferCrownModel, x, y + trunkHeight + 6.2f * scale, z,
                    angle, scale, scale, scale);
            add(coniferCrownModel, x, y + trunkHeight + 13f * scale, z,
                    angle + 24f, scale * 0.66f, scale * 0.68f, scale * 0.66f);
        } else {
            add(broadleafCrownModel, x, y + trunkHeight + 2.2f * scale, z,
                    angle, scale, scale, scale);
            add(broadleafCrownModel, x + 2.8f * scale, y + trunkHeight + 5f * scale,
                    z - 1.8f * scale, angle + 31f, scale * 0.72f, scale * 0.72f, scale * 0.72f);
            if (fruitTree) {
                add(fruitModel, x - 2.5f * scale, y + trunkHeight + 2.8f * scale, z,
                        angle, scale, scale, scale);
                add(fruitModel, x + 2f * scale, y + trunkHeight + 1.6f * scale,
                        z + 2.2f * scale, angle, scale, scale, scale);
            }
        }
    }

    private void addBush(int cellX, int cellZ, float x, float y, float z) {
        float scale = 0.65f + WorldGenerator.random01(cellX, cellZ, 47) * 0.7f;
        add(bushModel, x, y + 1.2f * scale, z,
                WorldGenerator.random01(cellX, cellZ, 49) * 360f,
                scale * 1.25f, scale, scale);
    }

    private void addRock(int cellX, int cellZ, float x, float y, float z) {
        float scale = 0.7f + WorldGenerator.random01(cellX, cellZ, 59) * 1.45f;
        boolean tall = WorldGenerator.random01(cellX, cellZ, 60) < 0.36f;
        Model selectedRock = tall ? tallRockModel : rockModel;
        add(selectedRock, x, y + (tall ? 1.55f : 0.9f) * scale, z,
                WorldGenerator.random01(cellX, cellZ, 61) * 360f,
                scale, scale * 0.72f, scale * 0.86f);
    }

    private void addGrass(int cellX, int cellZ, float x, float y, float z) {
        float scale = 0.55f + WorldGenerator.random01(cellX, cellZ, 67) * 0.55f;
        add(grassModel, x, y + 1.2f * scale, z,
                WorldGenerator.random01(cellX, cellZ, 71) * 360f,
                scale, scale, scale);
        add(grassModel, x + 1.1f * scale, y + scale, z - 0.7f * scale,
                WorldGenerator.random01(cellX, cellZ, 73) * 360f,
                scale * 0.72f, scale * 0.82f, scale * 0.72f);
        add(grassModel, x - 0.9f * scale, y + 0.85f * scale, z + scale,
                WorldGenerator.random01(cellX, cellZ, 79) * 360f,
                scale * 0.62f, scale * 0.70f, scale * 0.62f);
    }

    private void add(Model model, float x, float y, float z, float angle,
                     float scaleX, float scaleY, float scaleZ) {
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setToTranslation(x, y, z)
                .rotate(Vector3.Y, angle)
                .scale(scaleX, scaleY, scaleZ);
        cache.add(instance);
    }

    private static Material material(float red, float green, float blue) {
        return new Material(ColorAttribute.createDiffuse(new Color(red, green, blue, 1f)));
    }
}
