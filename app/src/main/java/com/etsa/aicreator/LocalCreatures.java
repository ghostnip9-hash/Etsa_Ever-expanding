package com.etsa.aicreator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

/** Smoothly animates small deterministic populations only on currently rendered tiles. */
final class LocalCreatures {
    private static final int LAND = 0;
    private static final int AIR = 1;
    private static final int WATER = 2;
    private static final int CREATURES_PER_KIND = 2;
    private static final int SEARCH_ATTEMPTS = 128;
    private static final int MAX_TILE_POPULATIONS = 3;
    private static final float STREAM_RADIUS = 900f;
    private static final float REBUILD_DISTANCE = 1_100f;
    private static final float TILE_MARGIN = 70f;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position
            | VertexAttributes.Usage.Normal;
    private static final int WYVERN_RIGID = 1;
    private static final int WYVERN_LEFT_WING = 2;
    private static final int WYVERN_RIGHT_WING = 3;
    private static final int WYVERN_TAIL_1 = 4;
    private static final int WYVERN_TAIL_2 = 5;
    private static final int WYVERN_TAIL_3 = 6;

    private final ArrayList<TilePopulation> populations = new ArrayList<>(MAX_TILE_POPULATIONS);

    private final ArrayList<Model> models = new ArrayList<>();
    private final Model behemothBody, behemothArmor, behemothHead, behemothHorn, behemothLeg;
    private final Model direfangBody, direfangHead, direfangLeg, direfangFang;
    private final Model wyvernBody, wyvernChest, wyvernHips, wyvernBelly, wyvernNeck;
    private final Model wyvernHead, wyvernJaw, wyvernMuzzle, wyvernLeftWing, wyvernRightWing;
    private final Model wyvernTailBase, wyvernTailMiddle, wyvernTailTip;
    private final Model wyvernHorn, wyvernSpine, wyvernLeg, wyvernFoot, wyvernClaw;
    private final Model wyvernEye, wyvernPupil;
    private final Model wraithCore, wraithShroud, wraithEye;
    private final Model squidMantle, squidHead, squidEye, squidArm, squidTentacle;
    private final Model crawlerBody, crawlerHead, crawlerLeg, crawlerEye;
    private float elapsedTime;

    LocalCreatures() {
        ModelBuilder builder = new ModelBuilder();
        behemothBody = model(builder.createSphere(22f, 12f, 14f, 14, 10, material(0.28f, 0.22f, 0.14f), ATTRIBUTES));
        behemothArmor = model(builder.createSphere(13f, 7f, 15f, 12, 8, material(0.43f, 0.35f, 0.21f), ATTRIBUTES));
        behemothHead = model(builder.createSphere(11f, 9f, 10f, 12, 9, material(0.34f, 0.27f, 0.16f), ATTRIBUTES));
        behemothHorn = model(builder.createCone(3.4f, 10f, 3.4f, 9, material(0.78f, 0.70f, 0.48f), ATTRIBUTES));
        behemothLeg = model(builder.createCylinder(4.5f, 9f, 4.5f, 10, material(0.20f, 0.17f, 0.11f), ATTRIBUTES));
        direfangBody = model(builder.createSphere(16f, 7f, 9f, 13, 9, material(0.30f, 0.12f, 0.11f), ATTRIBUTES));
        direfangHead = model(builder.createSphere(8f, 7f, 7f, 11, 8, material(0.46f, 0.18f, 0.13f), ATTRIBUTES));
        direfangLeg = model(builder.createCylinder(2.2f, 6f, 2.2f, 8, material(0.18f, 0.08f, 0.07f), ATTRIBUTES));
        direfangFang = model(builder.createCone(1.3f, 4.5f, 1.3f, 7, material(0.90f, 0.84f, 0.65f), ATTRIBUTES));
        wyvernBody = createWyvernHull(builder, 32f, 14f, 17f, 0.72f, 0.76f, 0.035f, 0.10f, 0.24f);
        wyvernChest = createWyvernHull(builder, 24f, 18f, 20f, 0.68f, 0.72f, 0.05f, 0.16f, 0.34f);
        wyvernHips = createWyvernHull(builder, 22f, 12f, 15f, 0.76f, 0.58f, 0.03f, 0.085f, 0.21f);
        wyvernBelly = createWyvernHull(builder, 24f, 7f, 11f, 0.66f, 0.62f, 0.76f, 0.72f, 0.57f);
        wyvernNeck = createWyvernHull(builder, 23f, 9f, 10f, 0.64f, 0.76f, 0.055f, 0.14f, 0.31f);
        wyvernHead = createWyvernHull(builder, 17f, 10f, 11f, 0.42f, 0.78f, 0.04f, 0.12f, 0.29f);
        wyvernJaw = createWyvernHull(builder, 15f, 4.5f, 8f, 0.28f, 0.72f, 0.78f, 0.74f, 0.60f);
        wyvernMuzzle = createWyvernHull(builder, 15f, 5.5f, 7f, 0.22f, 0.74f, 0.07f, 0.18f, 0.38f);
        wyvernLeftWing = createWyvernWing(builder, 1f);
        wyvernRightWing = createWyvernWing(builder, -1f);
        wyvernTailBase = createWyvernHull(builder, 26f, 8f, 9f, 0.58f, 0.88f, 0.04f, 0.11f, 0.25f);
        wyvernTailMiddle = createWyvernHull(builder, 23f, 5.8f, 6.5f, 0.45f, 0.76f, 0.045f, 0.12f, 0.27f);
        wyvernTailTip = createWyvernHull(builder, 24f, 3.8f, 4.2f, 0.08f, 0.66f, 0.05f, 0.13f, 0.29f);
        wyvernHorn = model(builder.createCone(2.5f, 11f, 2.5f, 9, material(0.18f, 0.22f, 0.28f), ATTRIBUTES));
        wyvernSpine = model(builder.createCone(3.2f, 10f, 3.2f, 8, material(0.08f, 0.16f, 0.30f), ATTRIBUTES));
        wyvernLeg = createWyvernHull(builder, 15f, 5f, 5f, 0.62f, 0.82f, 0.055f, 0.13f, 0.27f);
        wyvernFoot = createWyvernHull(builder, 12f, 4f, 6f, 0.42f, 0.78f, 0.06f, 0.14f, 0.28f);
        wyvernClaw = model(builder.createCone(1.8f, 7f, 1.8f, 7, material(0.83f, 0.78f, 0.61f), ATTRIBUTES));
        wyvernEye = model(builder.createSphere(2.5f, 2.5f, 2.5f, 10, 8, material(0.95f, 0.66f, 0.08f), ATTRIBUTES));
        wyvernPupil = model(builder.createSphere(1.0f, 1.8f, 1.0f, 8, 6, material(0.015f, 0.012f, 0.01f), ATTRIBUTES));
        wraithCore = model(builder.createSphere(10f, 5f, 7f, 12, 8, material(0.25f, 0.18f, 0.49f), ATTRIBUTES));
        wraithShroud = model(builder.createCone(13f, 18f, 13f, 12, material(0.39f, 0.30f, 0.68f), ATTRIBUTES));
        wraithEye = model(builder.createSphere(1.6f, 1.6f, 1.6f, 8, 6, material(0.67f, 0.95f, 1f), ATTRIBUTES));
        squidMantle = model(builder.createSphere(24f, 13f, 16f, 16, 11, material(0.30f, 0.10f, 0.36f), ATTRIBUTES));
        squidHead = model(builder.createSphere(16f, 11f, 14f, 15, 10, material(0.43f, 0.15f, 0.45f), ATTRIBUTES));
        squidEye = model(builder.createSphere(4.2f, 4.2f, 4.2f, 10, 8, material(0.95f, 0.78f, 0.20f), ATTRIBUTES));
        squidArm = model(builder.createSphere(15f, 2.4f, 3f, 10, 7, material(0.38f, 0.12f, 0.41f), ATTRIBUTES));
        squidTentacle = model(builder.createSphere(29f, 2.1f, 2.6f, 12, 7, material(0.53f, 0.18f, 0.51f), ATTRIBUTES));
        crawlerBody = model(builder.createSphere(16f, 6f, 13f, 13, 9, material(0.12f, 0.39f, 0.28f), ATTRIBUTES));
        crawlerHead = model(builder.createSphere(10f, 5f, 11f, 12, 8, material(0.18f, 0.53f, 0.35f), ATTRIBUTES));
        crawlerLeg = model(builder.createSphere(9f, 2f, 3f, 9, 6, material(0.09f, 0.29f, 0.21f), ATTRIBUTES));
        crawlerEye = model(builder.createSphere(2.2f, 2.2f, 2.2f, 8, 6, material(0.90f, 0.90f, 0.25f), ATTRIBUTES));
    }

    void update(float deltaSeconds, PerspectiveCamera camera, Vector3 focus,
                int activeTileX, int activeTileZ,
                boolean hasNeighbor, int neighborTileX, int neighborTileZ) {
        elapsedTime += Math.min(deltaSeconds, 0.05f);
        removeInvisiblePopulations(activeTileX, activeTileZ,
                hasNeighbor, neighborTileX, neighborTileZ);
        ensurePopulation(camera, activeTileX, activeTileZ, focus.x, focus.z, true);

        if (hasNeighbor && populations.size() < MAX_TILE_POPULATIONS) {
            float neighborAnchorX = clampToTile(focus.x, neighborTileX);
            float neighborAnchorZ = clampToTile(focus.z, neighborTileZ);
            ensurePopulation(camera, neighborTileX, neighborTileZ,
                    neighborAnchorX, neighborAnchorZ, false);
        }

        for (int populationIndex = 0; populationIndex < populations.size(); populationIndex++) {
            TilePopulation population = populations.get(populationIndex);
            for (int creatureIndex = 0; creatureIndex < population.creatures.size();
                 creatureIndex++) {
                population.creatures.get(creatureIndex).update(elapsedTime, deltaSeconds);
            }
        }
    }

    void render(ModelBatch batch, Environment environment) {
        for (int populationIndex = 0; populationIndex < populations.size(); populationIndex++) {
            ArrayList<Creature> creatures = populations.get(populationIndex).creatures;
            for (int creatureIndex = 0; creatureIndex < creatures.size(); creatureIndex++) {
                Creature creature = creatures.get(creatureIndex);
                for (int partIndex = 0; partIndex < creature.parts.size(); partIndex++) {
                    batch.render(creature.parts.get(partIndex).instance, environment);
                }
            }
        }
    }

    void dispose() {
        populations.clear();
        for (int index = 0; index < models.size(); index++) {
            models.get(index).dispose();
        }
    }

    private void removeInvisiblePopulations(int activeTileX, int activeTileZ,
                                            boolean hasNeighbor,
                                            int neighborTileX, int neighborTileZ) {
        for (int index = populations.size() - 1; index >= 0; index--) {
            TilePopulation population = populations.get(index);
            boolean active = population.tileX == activeTileX && population.tileZ == activeTileZ;
            boolean neighbor = hasNeighbor && population.tileX == neighborTileX
                    && population.tileZ == neighborTileZ;
            if (!active && !neighbor) {
                populations.remove(index);
            }
        }
    }

    private void ensurePopulation(PerspectiveCamera camera, int newTileX, int newTileZ,
                                  float anchorX, float anchorZ, boolean allowRebuild) {
        TilePopulation existing = findPopulation(newTileX, newTileZ);
        if (existing != null) {
            float dx = anchorX - existing.anchorX;
            float dz = anchorZ - existing.anchorZ;
            if (!allowRebuild || dx * dx + dz * dz < REBUILD_DISTANCE * REBUILD_DISTANCE) {
                return;
            }
            populations.remove(existing);
        }
        if (populations.size() >= MAX_TILE_POPULATIONS) {
            return;
        }
        populations.add(createPopulation(camera, newTileX, newTileZ, anchorX, anchorZ));
    }

    private TilePopulation findPopulation(int searchTileX, int searchTileZ) {
        for (int index = 0; index < populations.size(); index++) {
            TilePopulation population = populations.get(index);
            if (population.tileX == searchTileX && population.tileZ == searchTileZ) {
                return population;
            }
        }
        return null;
    }

    private TilePopulation createPopulation(PerspectiveCamera camera, int newTileX, int newTileZ,
                                            float anchorX, float anchorZ) {
        TilePopulation population = new TilePopulation(newTileX, newTileZ, anchorX, anchorZ);
        int anchorCellX = MathUtils.floor(anchorX / 520f);
        int anchorCellZ = MathUtils.floor(anchorZ / 520f);
        addAirCreatures(population, anchorCellX, anchorCellZ);

        int landCount = 0;
        int waterCount = 0;
        for (int attempt = 0; attempt < SEARCH_ATTEMPTS
                && (landCount < CREATURES_PER_KIND || waterCount < CREATURES_PER_KIND); attempt++) {
            float angle = WorldGenerator.random01(anchorCellX, anchorCellZ,
                    311 + attempt * 17) * MathUtils.PI2;
            float distance = 150f + WorldGenerator.random01(anchorCellX, anchorCellZ,
                    401 + attempt * 23) * (STREAM_RADIUS - 150f);
            float x = anchorX + MathUtils.cos(angle) * distance;
            float z = anchorZ + MathUtils.sin(angle) * distance;
            if (!insideTile(x, z, newTileX, newTileZ)) {
                continue;
            }
            float generatedHeight = WorldGenerator.height(x, z);
            float phase = WorldGenerator.random01(anchorCellX, anchorCellZ,
                    457 + attempt * 29) * MathUtils.PI2;
            if (landCount < CREATURES_PER_KIND
                    && generatedHeight > WorldGenerator.WATER_LEVEL + 5f
                    && WorldGenerator.slope(x, z) < 0.65f) {
                population.creatures.add(createLandCreature(x, z, phase, landCount));
                landCount++;
            } else if (waterCount < CREATURES_PER_KIND
                    && generatedHeight < WorldGenerator.WATER_LEVEL - 2.5f) {
                population.creatures.add(createWaterCreature(x, z, phase, waterCount));
                waterCount++;
            }
        }
        return population;
    }

    private void addAirCreatures(TilePopulation population, int anchorCellX, int anchorCellZ) {
        for (int index = 0; index < CREATURES_PER_KIND; index++) {
            float angle = WorldGenerator.random01(anchorCellX, anchorCellZ,
                    701 + index * 31) * MathUtils.PI2;
            float distance = 230f + WorldGenerator.random01(anchorCellX, anchorCellZ,
                    733 + index * 37) * 430f;
            float x = clampToTile(population.anchorX + MathUtils.cos(angle) * distance,
                    population.tileX);
            float z = clampToTile(population.anchorZ + MathUtils.sin(angle) * distance,
                    population.tileZ);
            float phase = WorldGenerator.random01(anchorCellX, anchorCellZ,
                    809 + index * 43) * MathUtils.PI2;
            population.creatures.add(createAirCreature(x, z, phase, index));
        }
    }

    private Creature createLandCreature(float x, float z, float phase, int index) {
        return index == 0 ? createHornedBehemoth(x, z, phase) : createDirefang(x, z, phase);
    }

    private Creature createHornedBehemoth(float x, float z, float phase) {
        Creature creature = new Creature(LAND, x, z, phase, 12f, 0.34f);
        creature.add(behemothBody, 0f, 10f, 0f, 1f, 1f, 1f, false);
        creature.add(behemothArmor, -5f, 15f, 0f, 1f, 1f, 1f, false);
        creature.add(behemothHead, 14f, 11f, 0f, 1f, 1f, 1f, false);
        creature.add(behemothHorn, 18f, 18f, -4f, 1f, 1f, 1f, false);
        creature.add(behemothHorn, 18f, 18f, 4f, 1f, 1f, 1f, false);
        creature.add(behemothHorn, 9f, 20f, 0f, 0.75f, 0.8f, 0.75f, false);
        creature.add(behemothLeg, -8f, 4.5f, -6f, 1f, 1f, 1f, true);
        creature.add(behemothLeg, 8f, 4.5f, -6f, 1f, 1f, 1f, true);
        creature.add(behemothLeg, -8f, 4.5f, 6f, 1f, 1f, 1f, true);
        creature.add(behemothLeg, 8f, 4.5f, 6f, 1f, 1f, 1f, true);
        return creature;
    }

    private Creature createDirefang(float x, float z, float phase) {
        Creature creature = new Creature(LAND, x, z, phase, 12f, 0.34f);
        creature.add(direfangBody, 0f, 7f, 0f, 1f, 1f, 1f, false);
        creature.add(direfangBody, -10f, 7.5f, 0f, 0.62f, 0.62f, 0.62f, false);
        creature.add(direfangHead, 12f, 9f, 0f, 1f, 1f, 1f, false);
        creature.add(direfangHead, 17f, 7.5f, 0f, 0.65f, 0.55f, 0.72f, false);
        creature.add(direfangFang, 18f, 4.5f, -2.5f, 1f, 1f, 1f, false);
        creature.add(direfangFang, 18f, 4.5f, 2.5f, 1f, 1f, 1f, false);
        creature.add(direfangLeg, -7f, 3f, -4f, 1f, 1f, 1f, true);
        creature.add(direfangLeg, 7f, 3f, -4f, 1f, 1f, 1f, true);
        creature.add(direfangLeg, -7f, 3f, 4f, 1f, 1f, 1f, true);
        creature.add(direfangLeg, 7f, 3f, 4f, 1f, 1f, 1f, true);
        return creature;
    }

    private Creature createAirCreature(float x, float z, float phase, int index) {
        return index == 0 ? createWyvern(x, z, phase, index) : createSkyWraith(x, z, phase, index);
    }

    private Creature createWyvern(float x, float z, float phase, int index) {
        Creature creature = new Creature(AIR, x, z, phase, 78f + index * 12f, 0.50f);
        creature.wyvern = true;

        creature.addWyvern(wyvernBody, -1f, 1f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 0f);
        creature.addWyvern(wyvernChest, 11f, 4f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 5f);
        creature.addWyvern(wyvernHips, -14f, 0f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -4f);
        creature.addWyvern(wyvernBelly, 5f, -4f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 4f);

        creature.addWyvern(wyvernNeck, 21f, 9f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 25f);
        creature.addWyvern(wyvernNeck, 30f, 18f, 0f, 0.82f, 0.86f, 0.86f, WYVERN_RIGID, 0f, 0f, 34f);
        creature.addWyvern(wyvernHead, 39f, 24f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -7f);
        creature.addWyvern(wyvernMuzzle, 50f, 22f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -5f);
        creature.addWyvern(wyvernJaw, 49f, 17.5f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -9f);
        creature.addWyvern(wyvernEye, 42f, 26f, -5.2f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 0f);
        creature.addWyvern(wyvernEye, 42f, 26f, 5.2f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 0f);
        creature.addWyvern(wyvernPupil, 43f, 26f, -6.25f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 0f);
        creature.addWyvern(wyvernPupil, 43f, 26f, 6.25f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 0f);

        creature.addWyvern(wyvernHorn, 36f, 32f, -4.5f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -34f);
        creature.addWyvern(wyvernHorn, 36f, 32f, 4.5f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -34f);
        creature.addWyvern(wyvernSpine, 27f, 27f, 0f, 0.8f, 0.9f, 0.8f, WYVERN_RIGID, 0f, 0f, -18f);
        creature.addWyvern(wyvernSpine, 17f, 20f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -12f);
        creature.addWyvern(wyvernSpine, 0f, 14f, 0f, 1.15f, 1.15f, 1.15f, WYVERN_RIGID, 0f, 0f, 0f);
        creature.addWyvern(wyvernSpine, -12f, 10f, 0f, 1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, 8f);

        creature.addWyvern(wyvernLeftWing, 7f, 10f, 6f, 1f, 1f, 1f, WYVERN_LEFT_WING, 0f, 0f, 0f);
        creature.addWyvern(wyvernRightWing, 7f, 10f, -6f, 1f, 1f, 1f, WYVERN_RIGHT_WING, 0f, 0f, 0f);

        addWyvernLeg(creature, -6f, -7f, -9f);
        addWyvernLeg(creature, -6f, -7f, 9f);

        creature.addWyvern(wyvernTailBase, -25f, -1f, 0f, 1f, 1f, 1f, WYVERN_TAIL_1, 0f, 0f, 2f);
        creature.addWyvern(wyvernTailMiddle, -47f, -1.5f, 0f, 1f, 1f, 1f, WYVERN_TAIL_2, 0f, 0f, 2f);
        creature.addWyvern(wyvernTailTip, -68f, -1f, 0f, 1f, 1f, 1f, WYVERN_TAIL_3, 0f, 0f, 3f);
        creature.addWyvern(wyvernSpine, -23f, 7f, 0f, 0.8f, 0.8f, 0.8f, WYVERN_TAIL_1, 0f, 0f, 10f);
        creature.addWyvern(wyvernSpine, -37f, 5f, 0f, 0.65f, 0.65f, 0.65f, WYVERN_TAIL_2, 0f, 0f, 14f);
        creature.addWyvern(wyvernSpine, -50f, 3f, 0f, 0.5f, 0.5f, 0.5f, WYVERN_TAIL_3, 0f, 0f, 18f);
        return creature;
    }

    private void addWyvernLeg(Creature creature, float forward, float up, float side) {
        creature.addWyvern(wyvernLeg, forward, up, side, 1f, 1f, 1f,
                WYVERN_RIGID, 0f, 0f, -55f);
        creature.addWyvern(wyvernLeg, forward - 7f, up - 9f, side, 0.82f, 0.82f, 0.82f,
                WYVERN_RIGID, 0f, 0f, 62f);
        creature.addWyvern(wyvernFoot, forward - 3f, up - 16f, side, 1f, 1f, 1f,
                WYVERN_RIGID, 0f, 0f, -8f);
        float outward = side < 0f ? -1f : 1f;
        creature.addWyvern(wyvernClaw, forward + 2f, up - 19f, side - outward * 3.2f,
                1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -68f);
        creature.addWyvern(wyvernClaw, forward + 3f, up - 19.5f, side,
                1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -68f);
        creature.addWyvern(wyvernClaw, forward + 2f, up - 19f, side + outward * 3.2f,
                1f, 1f, 1f, WYVERN_RIGID, 0f, 0f, -68f);
    }

    private Creature createSkyWraith(float x, float z, float phase, int index) {
        Creature creature = new Creature(AIR, x, z, phase, 78f + index * 12f, 0.50f);
        creature.add(wraithCore, 5f, 2f, 0f, 1f, 1f, 1f, false);
        creature.add(wraithShroud, -2f, -5f, 0f, 1f, 1f, 1f, false);
        creature.add(wraithShroud, -10f, -8f, -5f, 0.55f, 0.9f, 0.55f, false);
        creature.add(wraithShroud, -10f, -8f, 5f, 0.55f, 0.9f, 0.55f, false);
        creature.add(wraithEye, 12f, 4f, -2.8f, 1f, 1f, 1f, false);
        creature.add(wraithEye, 12f, 4f, 2.8f, 1f, 1f, 1f, false);
        return creature;
    }

    private Creature createWaterCreature(float x, float z, float phase, int index) {
        return index == 0 ? createAbyssalGiantSquid(x, z, phase) : createMarshCrawler(x, z, phase);
    }

    private Creature createAbyssalGiantSquid(float x, float z, float phase) {
        Creature creature = new Creature(WATER, x, z, phase, 15f, 0.38f);
        creature.add(squidMantle, -8f, 2f, 0f, 1f, 1f, 1f, false);
        creature.add(squidMantle, -23f, 3f, 0f, 0.68f, 0.72f, 0.72f, false);
        creature.add(squidHead, 10f, 0f, 0f, 1f, 1f, 1f, false);
        creature.add(squidEye, 17f, 4f, -7f, 1f, 1f, 1f, false);
        creature.add(squidEye, 17f, 4f, 7f, 1f, 1f, 1f, false);
        creature.add(squidTentacle, 34f, -2f, -10f, 1f, 1f, 1f, false);
        creature.add(squidTentacle, 34f, -2f, 10f, 1f, 1f, 1f, false);
        creature.add(squidArm, 25f, -4f, -7f, 1f, 1f, 1f, false);
        creature.add(squidArm, 25f, -5f, -3f, 1f, 1f, 1f, false);
        creature.add(squidArm, 25f, -5f, 3f, 1f, 1f, 1f, false);
        creature.add(squidArm, 25f, -4f, 7f, 1f, 1f, 1f, false);
        creature.add(squidArm, 21f, -7f, -11f, 0.85f, 0.85f, 0.85f, false);
        creature.add(squidArm, 21f, -7f, 11f, 0.85f, 0.85f, 0.85f, false);
        return creature;
    }

    private Creature createMarshCrawler(float x, float z, float phase) {
        Creature creature = new Creature(WATER, x, z, phase, 15f, 0.38f);
        creature.add(crawlerBody, 0f, 0f, 0f, 1f, 1f, 1f, false);
        creature.add(crawlerHead, 12f, 1f, 0f, 1f, 1f, 1f, false);
        creature.add(crawlerEye, 16f, 5f, -4f, 1f, 1f, 1f, false);
        creature.add(crawlerEye, 16f, 5f, 4f, 1f, 1f, 1f, false);
        creature.add(crawlerLeg, -5f, -2f, -10f, 1f, 1f, 1f, false);
        creature.add(crawlerLeg, 7f, -2f, -10f, 1f, 1f, 1f, false);
        creature.add(crawlerLeg, -5f, -2f, 10f, 1f, 1f, 1f, false);
        creature.add(crawlerLeg, 7f, -2f, 10f, 1f, 1f, 1f, false);
        return creature;
    }

    private static float clampToTile(float coordinate, int coordinateTile) {
        float center = WorldCoordinates.tileCenter(coordinateTile);
        float limit = WorldCoordinates.HALF_TILE_SIZE - TILE_MARGIN;
        return MathUtils.clamp(coordinate, center - limit, center + limit);
    }

    private static boolean insideTile(float x, float z, int insideTileX, int insideTileZ) {
        float limit = WorldCoordinates.HALF_TILE_SIZE - TILE_MARGIN;
        return Math.abs(WorldCoordinates.localCoordinate(x, insideTileX)) <= limit
                && Math.abs(WorldCoordinates.localCoordinate(z, insideTileZ)) <= limit;
    }

    private Model model(Model model) {
        models.add(model);
        return model;
    }

    private Model createWyvernHull(ModelBuilder builder, float length, float height,
                                    float width, float frontTaper, float rearTaper,
                                    float red, float green, float blue) {
        builder.begin();
        MeshPartBuilder hull = builder.part("wyvern-hull", GL20.GL_TRIANGLES,
                ATTRIBUTES, material(red, green, blue));
        final int sides = 8;
        final int rings = 5;
        float[] positions = {-0.5f, -0.27f, 0f, 0.28f, 0.5f};
        float[] scales = {rearTaper, 0.92f, 1f, 0.91f, frontTaper};
        Vector3[][] vertices = new Vector3[rings][sides];
        for (int ring = 0; ring < rings; ring++) {
            for (int sideIndex = 0; sideIndex < sides; sideIndex++) {
                float angle = MathUtils.PI2 * sideIndex / sides;
                vertices[ring][sideIndex] = new Vector3(
                        positions[ring] * length,
                        MathUtils.cos(angle) * height * 0.5f * scales[ring],
                        MathUtils.sin(angle) * width * 0.5f * scales[ring]);
            }
        }
        for (int ring = 0; ring < rings - 1; ring++) {
            for (int sideIndex = 0; sideIndex < sides; sideIndex++) {
                int next = (sideIndex + 1) % sides;
                addDoubleTriangle(hull, vertices[ring][sideIndex],
                        vertices[ring + 1][sideIndex], vertices[ring + 1][next]);
                addDoubleTriangle(hull, vertices[ring][sideIndex],
                        vertices[ring + 1][next], vertices[ring][next]);
            }
        }
        Vector3 rearCenter = new Vector3(-length * 0.5f, 0f, 0f);
        Vector3 frontCenter = new Vector3(length * 0.5f, 0f, 0f);
        for (int sideIndex = 0; sideIndex < sides; sideIndex++) {
            int next = (sideIndex + 1) % sides;
            addDoubleTriangle(hull, rearCenter, vertices[0][next], vertices[0][sideIndex]);
            addDoubleTriangle(hull, frontCenter, vertices[rings - 1][sideIndex],
                    vertices[rings - 1][next]);
        }
        return model(builder.end());
    }

    private Model createWyvernWing(ModelBuilder builder, float side) {
        builder.begin();
        Vector3 root = new Vector3(0f, 0f, 0f);
        Vector3 elbow = new Vector3(10f, 7f, side * 18f);
        Vector3 wrist = new Vector3(5f, 10f, side * 34f);
        Vector3 primaryTip = new Vector3(-3f, 7f, side * 61f);
        Vector3 secondTip = new Vector3(-12f, 3f, side * 54f);
        Vector3 thirdTip = new Vector3(-20f, 0f, side * 45f);
        Vector3 fourthTip = new Vector3(-25f, -2f, side * 34f);
        Vector3 innerNotch = new Vector3(-17f, -2f, side * 20f);
        Vector3 rearRoot = new Vector3(-10f, 0f, side * 7f);

        MeshPartBuilder membrane = builder.part("wyvern-membrane", GL20.GL_TRIANGLES,
                ATTRIBUTES, material(0.70f, 0.47f, 0.22f));
        addDoubleTriangle(membrane, root, elbow, rearRoot);
        addDoubleTriangle(membrane, elbow, innerNotch, rearRoot);
        addDoubleTriangle(membrane, elbow, wrist, innerNotch);
        addDoubleTriangle(membrane, wrist, fourthTip, innerNotch);
        addDoubleTriangle(membrane, wrist, thirdTip, fourthTip);
        addDoubleTriangle(membrane, wrist, secondTip, thirdTip);
        addDoubleTriangle(membrane, wrist, primaryTip, secondTip);

        MeshPartBuilder bones = builder.part("wyvern-wing-bones", GL20.GL_TRIANGLES,
                ATTRIBUTES, material(0.035f, 0.10f, 0.23f));
        addWingRibbon(bones, root, elbow, 2.2f);
        addWingRibbon(bones, elbow, wrist, 1.9f);
        addWingRibbon(bones, wrist, primaryTip, 1.35f);
        addWingRibbon(bones, wrist, secondTip, 1.15f);
        addWingRibbon(bones, wrist, thirdTip, 1.0f);
        addWingRibbon(bones, wrist, fourthTip, 0.85f);
        return model(builder.end());
    }

    private static void addWingRibbon(MeshPartBuilder mesh, Vector3 start, Vector3 end,
                                      float width) {
        float dx = end.x - start.x;
        float dz = end.z - start.z;
        float inverseLength = 1f / Math.max(0.001f, (float) Math.sqrt(dx * dx + dz * dz));
        float offsetX = -dz * inverseLength * width;
        float offsetZ = dx * inverseLength * width;
        Vector3 a = new Vector3(start.x + offsetX, start.y + 0.35f, start.z + offsetZ);
        Vector3 b = new Vector3(start.x - offsetX, start.y + 0.35f, start.z - offsetZ);
        Vector3 c = new Vector3(end.x - offsetX, end.y + 0.35f, end.z - offsetZ);
        Vector3 d = new Vector3(end.x + offsetX, end.y + 0.35f, end.z + offsetZ);
        addDoubleTriangle(mesh, a, b, c);
        addDoubleTriangle(mesh, a, c, d);
    }

    private static void addDoubleTriangle(MeshPartBuilder mesh, Vector3 first,
                                          Vector3 second, Vector3 third) {
        mesh.triangle(first, second, third);
        mesh.triangle(third, second, first);
    }

    private static Material material(float red, float green, float blue) {
        return new Material(ColorAttribute.createDiffuse(new Color(red, green, blue, 1f)));
    }

    private static final class TilePopulation {
        final int tileX;
        final int tileZ;
        final float anchorX;
        final float anchorZ;
        final ArrayList<Creature> creatures = new ArrayList<>(CREATURES_PER_KIND * 3);

        TilePopulation(int tileX, int tileZ, float anchorX, float anchorZ) {
            this.tileX = tileX;
            this.tileZ = tileZ;
            this.anchorX = anchorX;
            this.anchorZ = anchorZ;
        }
    }

    private static final class Creature {
        final int habitat;
        final float originX;
        final float originZ;
        final float phase;
        final float movementRadius;
        final float speed;
        final ArrayList<CreaturePart> parts = new ArrayList<>(12);
        float headingDegrees;
        boolean headingInitialized;
        boolean wyvern;
        float bankDegrees;

        Creature(int habitat, float originX, float originZ, float phase,
                 float movementRadius, float speed) {
            this.habitat = habitat;
            this.originX = originX;
            this.originZ = originZ;
            this.phase = phase;
            this.movementRadius = movementRadius;
            this.speed = speed;
        }

        void add(Model model, float forward, float up, float side,
                 float scaleX, float scaleY, float scaleZ, boolean grounded) {
            parts.add(new CreaturePart(model, forward, up, side,
                    scaleX, scaleY, scaleZ, grounded));
        }

        void addWyvern(Model model, float forward, float up, float side,
                        float scaleX, float scaleY, float scaleZ, int animationRole,
                        float rotationX, float rotationY, float rotationZ) {
            parts.add(new CreaturePart(model, forward, up, side,
                    scaleX, scaleY, scaleZ, false, animationRole,
                    rotationX, rotationY, rotationZ));
        }

        void update(float time, float deltaSeconds) {
            float motion = time * speed + phase;
            float x = originX + MathUtils.cos(motion) * movementRadius;
            float z = originZ + MathUtils.sin(motion * 0.83f) * movementRadius;
            float velocityX = -MathUtils.sin(motion) * movementRadius * speed;
            float velocityZ = MathUtils.cos(motion * 0.83f)
                    * movementRadius * speed * 0.83f;
            float desiredHeading = MathUtils.atan2(velocityX, velocityZ)
                    * MathUtils.radiansToDegrees;
            float headingError = 0f;
            if (!headingInitialized) {
                headingDegrees = desiredHeading;
                headingInitialized = true;
            } else {
                headingError = MathUtils.atan2(MathUtils.sinDeg(desiredHeading - headingDegrees),
                        MathUtils.cosDeg(desiredHeading - headingDegrees)) * MathUtils.radiansToDegrees;
                float turnResponse = habitat == AIR ? 1.6f : habitat == WATER ? 2.4f : 3.2f;
                headingDegrees = MathUtils.lerpAngleDeg(headingDegrees, desiredHeading,
                        MathUtils.clamp(deltaSeconds * turnResponse, 0f, 1f));
            }
            if (wyvern) {
                float targetBank = MathUtils.clamp(-headingError * 0.22f, -13f, 13f);
                bankDegrees = MathUtils.lerp(bankDegrees, targetBank,
                        MathUtils.clamp(deltaSeconds * 2.2f, 0f, 1f));
            }
            float baseY;
            if (habitat == AIR) {
                float surface = Math.max(WorldGenerator.WATER_LEVEL,
                        EtsaWorld.terrainSurfaceHeight(x, z));
                baseY = surface + 52f + MathUtils.sin(time * 0.9f + phase) * 8f;
            } else if (habitat == WATER) {
                baseY = WorldGenerator.WATER_LEVEL + 0.8f
                        + MathUtils.sin(time * 1.15f + phase) * 0.45f;
            } else {
                baseY = EtsaWorld.terrainSurfaceHeight(x, z)
                        + MathUtils.sin(time * 2f + phase) * 0.3f;
            }

            float sinYaw = MathUtils.sinDeg(headingDegrees);
            float cosYaw = MathUtils.cosDeg(headingDegrees);
            for (int index = 0; index < parts.size(); index++) {
                CreaturePart part = parts.get(index);
                float animatedSide = part.side;
                float animatedUp = part.up;
                if (wyvern) {
                    float bankSin = MathUtils.sinDeg(bankDegrees);
                    float bankCos = MathUtils.cosDeg(bankDegrees);
                    animatedSide = part.side * bankCos - part.up * bankSin;
                    animatedUp = part.side * bankSin + part.up * bankCos;
                }
                float partX = x + sinYaw * part.forward + cosYaw * animatedSide;
                float partZ = z + cosYaw * part.forward - sinYaw * animatedSide;
                float partY = part.grounded
                        ? EtsaWorld.terrainSurfaceHeight(partX, partZ) + animatedUp
                        : baseY + animatedUp;
                if (habitat == LAND && part.grounded) {
                    partY += Math.max(0f, MathUtils.sin(time * 2.8f + phase
                            + part.forward * 0.45f + part.side * 0.3f)) * 0.9f;
                } else if (habitat == AIR && Math.abs(part.side) > 8f) {
                    partY += MathUtils.sin(time * 2.3f + phase) * 1.8f;
                } else if (habitat == WATER && Math.abs(part.side) > 3f) {
                    partY += MathUtils.sin(time * 2f + phase + part.side) * 0.7f;
                }
                float wingRotation = 0f;
                float tailRotation = 0f;
                if (wyvern) {
                    float glideEnvelope = 0.28f + 0.72f
                            * (0.5f + 0.5f * MathUtils.sin(time * 0.47f + phase));
                    float flap = MathUtils.sin(time * 3.2f + phase) * 31f * glideEnvelope;
                    if (part.animationRole == WYVERN_LEFT_WING) {
                        wingRotation = flap;
                    } else if (part.animationRole == WYVERN_RIGHT_WING) {
                        wingRotation = -flap;
                    } else if (part.animationRole >= WYVERN_TAIL_1) {
                        float tailPhase = (part.animationRole - WYVERN_TAIL_1) * 0.55f;
                        tailRotation = MathUtils.sin(time * 1.35f + phase - tailPhase)
                                * (part.animationRole - WYVERN_TAIL_1 + 1f) * 2.2f;
                    }
                }
                part.instance.transform.setToTranslation(partX, partY, partZ)
                        .rotate(Vector3.Y, headingDegrees)
                        .rotate(Vector3.X, wyvern ? bankDegrees : 0f)
                        .rotate(Vector3.X, part.rotationX + wingRotation)
                        .rotate(Vector3.Y, part.rotationY + tailRotation)
                        .rotate(Vector3.Z, part.rotationZ)
                        .scale(part.scaleX, part.scaleY, part.scaleZ);
            }
        }
    }

    private static final class CreaturePart {
        final ModelInstance instance;
        final float forward;
        final float up;
        final float side;
        final float scaleX;
        final float scaleY;
        final float scaleZ;
        final boolean grounded;
        final int animationRole;
        final float rotationX;
        final float rotationY;
        final float rotationZ;

        CreaturePart(Model model, float forward, float up, float side,
                     float scaleX, float scaleY, float scaleZ, boolean grounded) {
            this(model, forward, up, side, scaleX, scaleY, scaleZ, grounded,
                    0, 0f, 0f, 0f);
        }

        CreaturePart(Model model, float forward, float up, float side,
                     float scaleX, float scaleY, float scaleZ, boolean grounded,
                     int animationRole, float rotationX, float rotationY, float rotationZ) {
            instance = new ModelInstance(model);
            this.forward = forward;
            this.up = up;
            this.side = side;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.grounded = grounded;
            this.animationRole = animationRole;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
        }
    }
}
