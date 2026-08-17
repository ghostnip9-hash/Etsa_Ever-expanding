package com.etsa.aicreator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
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

    private final ArrayList<TilePopulation> populations = new ArrayList<>(MAX_TILE_POPULATIONS);

    private final Model landBody;
    private final Model landHead;
    private final Model landHorn;
    private final Model landLeg;
    private final Model landSpine;
    private final Model airBody;
    private final Model airWing;
    private final Model airEye;
    private final Model airCrest;
    private final Model waterBody;
    private final Model waterFin;
    private final Model waterEye;
    private float elapsedTime;

    LocalCreatures() {
        ModelBuilder builder = new ModelBuilder();
        landBody = builder.createSphere(15f, 7f, 9f, 12, 9,
                material(0.24f, 0.43f, 0.16f), ATTRIBUTES);
        landHead = builder.createSphere(7.5f, 6.5f, 7.5f, 10, 8,
                material(0.40f, 0.57f, 0.21f), ATTRIBUTES);
        landHorn = builder.createCone(2.2f, 7f, 2.2f, 8,
                material(0.72f, 0.67f, 0.44f), ATTRIBUTES);
        landLeg = builder.createCylinder(2.3f, 5f, 2.3f, 8,
                material(0.18f, 0.31f, 0.12f), ATTRIBUTES);
        landSpine = builder.createCone(2.8f, 5.5f, 2.8f, 7,
                material(0.52f, 0.68f, 0.25f), ATTRIBUTES);

        airBody = builder.createSphere(13f, 4.2f, 16f, 12, 8,
                material(0.27f, 0.19f, 0.58f), ATTRIBUTES);
        airWing = builder.createSphere(16f, 2.2f, 9f, 11, 7,
                material(0.50f, 0.35f, 0.78f), ATTRIBUTES);
        airEye = builder.createSphere(1.9f, 1.9f, 1.9f, 8, 6,
                material(0.76f, 0.96f, 1f), ATTRIBUTES);
        airCrest = builder.createCone(3.2f, 7f, 3.2f, 8,
                material(0.68f, 0.48f, 0.88f), ATTRIBUTES);

        waterBody = builder.createSphere(17f, 6.5f, 10f, 12, 9,
                material(0.07f, 0.47f, 0.50f), ATTRIBUTES);
        waterFin = builder.createCone(5f, 9f, 5f, 8,
                material(0.10f, 0.72f, 0.61f), ATTRIBUTES);
        waterEye = builder.createSphere(2.1f, 2.1f, 2.1f, 8, 6,
                material(0.94f, 0.81f, 0.20f), ATTRIBUTES);
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
        landBody.dispose();
        landHead.dispose();
        landHorn.dispose();
        landLeg.dispose();
        landSpine.dispose();
        airBody.dispose();
        airWing.dispose();
        airEye.dispose();
        airCrest.dispose();
        waterBody.dispose();
        waterFin.dispose();
        waterEye.dispose();
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
                population.creatures.add(createLandProwler(x, z, phase));
                landCount++;
            } else if (waterCount < CREATURES_PER_KIND
                    && generatedHeight < WorldGenerator.WATER_LEVEL - 2.5f) {
                population.creatures.add(createReefBeast(x, z, phase));
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
            population.creatures.add(createSkyRay(x, z, phase, index));
        }
    }

    private Creature createLandProwler(float x, float z, float phase) {
        Creature creature = new Creature(LAND, x, z, phase, 12f, 0.34f);
        creature.add(landBody, 0f, 5.5f, 0f, 1f, 1f, 1f, false);
        creature.add(landHead, 7.2f, 7.3f, 0f, 1f, 1f, 1f, false);
        creature.add(landHorn, 8f, 12.2f, 0f, 1f, 1f, 1f, false);
        creature.add(landHorn, 5.7f, 10.8f, -2.3f, 0.72f, 0.78f, 0.72f, false);
        creature.add(landHorn, 5.7f, 10.8f, 2.3f, 0.72f, 0.78f, 0.72f, false);
        creature.add(landSpine, -1f, 10f, 0f, 1f, 1f, 1f, false);
        creature.add(landSpine, -5f, 9f, 0f, 0.78f, 0.78f, 0.78f, false);
        creature.add(landLeg, -4.5f, 2.5f, -2.8f, 1f, 1f, 1f, true);
        creature.add(landLeg, 4.5f, 2.5f, -2.8f, 1f, 1f, 1f, true);
        creature.add(landLeg, -4.5f, 2.5f, 2.8f, 1f, 1f, 1f, true);
        creature.add(landLeg, 4.5f, 2.5f, 2.8f, 1f, 1f, 1f, true);
        return creature;
    }

    private Creature createSkyRay(float x, float z, float phase, int index) {
        Creature creature = new Creature(AIR, x, z, phase, 78f + index * 12f, 0.50f);
        creature.add(airBody, 0f, 0f, 0f, 1f, 1f, 1f, false);
        creature.add(airWing, 0f, 0f, -10f, 1f, 1f, 1f, false);
        creature.add(airWing, 0f, 0f, 10f, 1f, 1f, 1f, false);
        creature.add(airEye, 6.2f, 0.7f, -2.1f, 1f, 1f, 1f, false);
        creature.add(airEye, 6.2f, 0.7f, 2.1f, 1f, 1f, 1f, false);
        creature.add(airCrest, -1f, 5f, 0f, 1f, 1f, 1f, false);
        creature.add(airCrest, -6f, 3.5f, 0f, 0.7f, 0.7f, 0.7f, false);
        return creature;
    }

    private Creature createReefBeast(float x, float z, float phase) {
        Creature creature = new Creature(WATER, x, z, phase, 15f, 0.38f);
        creature.add(waterBody, 0f, 0f, 0f, 1f, 1f, 1f, false);
        creature.add(waterFin, 0f, 5.3f, 0f, 1f, 1f, 1f, false);
        creature.add(waterFin, -5f, 4f, 0f, 0.72f, 0.72f, 0.72f, false);
        creature.add(waterEye, 7f, 2.5f, -2.7f, 1f, 1f, 1f, false);
        creature.add(waterEye, 7f, 2.5f, 2.7f, 1f, 1f, 1f, false);
        creature.add(waterFin, -8f, 0f, -4f, 0.65f, 0.65f, 0.65f, false);
        creature.add(waterFin, -8f, 0f, 4f, 0.65f, 0.65f, 0.65f, false);
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

        void update(float time, float deltaSeconds) {
            float motion = time * speed + phase;
            float x = originX + MathUtils.cos(motion) * movementRadius;
            float z = originZ + MathUtils.sin(motion * 0.83f) * movementRadius;
            float velocityX = -MathUtils.sin(motion) * movementRadius * speed;
            float velocityZ = MathUtils.cos(motion * 0.83f)
                    * movementRadius * speed * 0.83f;
            float desiredHeading = MathUtils.atan2(velocityX, velocityZ)
                    * MathUtils.radiansToDegrees;
            if (!headingInitialized) {
                headingDegrees = desiredHeading;
                headingInitialized = true;
            } else {
                float turnResponse = habitat == AIR ? 1.6f : habitat == WATER ? 2.4f : 3.2f;
                headingDegrees = MathUtils.lerpAngleDeg(headingDegrees, desiredHeading,
                        MathUtils.clamp(deltaSeconds * turnResponse, 0f, 1f));
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
                float partX = x + sinYaw * part.forward + cosYaw * part.side;
                float partZ = z + cosYaw * part.forward - sinYaw * part.side;
                float partY = part.grounded
                        ? EtsaWorld.terrainSurfaceHeight(partX, partZ) + part.up
                        : baseY + part.up;
                if (habitat == LAND && part.grounded) {
                    partY += Math.max(0f, MathUtils.sin(time * 2.8f + phase
                            + part.forward * 0.45f + part.side * 0.3f)) * 0.9f;
                } else if (habitat == AIR && Math.abs(part.side) > 8f) {
                    partY += MathUtils.sin(time * 2.3f + phase) * 1.8f;
                } else if (habitat == WATER && Math.abs(part.side) > 3f) {
                    partY += MathUtils.sin(time * 2f + phase + part.side) * 0.7f;
                }
                part.instance.transform.setToTranslation(partX, partY, partZ)
                        .rotate(Vector3.Y, headingDegrees)
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

        CreaturePart(Model model, float forward, float up, float side,
                     float scaleX, float scaleY, float scaleZ, boolean grounded) {
            instance = new ModelInstance(model);
            this.forward = forward;
            this.up = up;
            this.side = side;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.grounded = grounded;
        }
    }
}
