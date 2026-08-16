package com.etsa.aicreator;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;

/** Seeded local-region renderer for the natural-world milestone. */
public final class EtsaWorld extends ApplicationAdapter {
    private static final int GRID_CELLS = 192;
    private static final float TERRAIN_SIZE = WorldCoordinates.TILE_SIZE;
    private static final int WATER_GRID_CELLS = 40;
    private static final int FLOATS_PER_VERTEX = 7;

    private PerspectiveCamera camera;
    private RtsCameraController cameraController;
    private ModelBatch modelBatch;
    private Model terrainModel;
    private Model neighborTerrainModel;
    private Model waterModel;
    private ModelInstance terrainInstance;
    private ModelInstance neighborTerrainInstance;
    private ModelInstance waterInstance;
    private ModelInstance neighborWaterInstance;
    private LocalEnvironment localEnvironment;
    private LocalCreatures localCreatures;
    private WorldMinimap worldMinimap;
    private WorldCoordinateOverlay coordinateOverlay;
    private PersistentWorldState worldState;
    private final Vector3 cameraFocus = new Vector3();
    private Environment environment;
    private float waterTime;
    private int tileX;
    private int tileZ;
    private int neighborTileX = Integer.MIN_VALUE;
    private int neighborTileZ = Integer.MIN_VALUE;

    @Override
    public void create() {
        camera = new PerspectiveCamera(55f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraController = new RtsCameraController(camera);
        worldState = new PersistentWorldState();
        cameraController.setTarget(worldState.playerX(), worldState.playerZ());
        tileX = WorldCoordinates.tileIndex(worldState.playerX());
        tileZ = WorldCoordinates.tileIndex(worldState.playerZ());
        Gdx.input.setInputProcessor(new GestureDetector(cameraController));

        modelBatch = new ModelBatch();
        terrainModel = createTerrainModel(tileX, tileZ);
        terrainInstance = new ModelInstance(terrainModel);
        terrainInstance.transform.setToTranslation(
                WorldCoordinates.tileCenter(tileX), 0f, WorldCoordinates.tileCenter(tileZ));
        waterModel = createWaterModel();
        waterInstance = new ModelInstance(waterModel);
        localEnvironment = new LocalEnvironment();
        localEnvironment.setTile(tileX, tileZ);
        localCreatures = new LocalCreatures();
        worldMinimap = new WorldMinimap();
        coordinateOverlay = new WorldCoordinateOverlay();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.44f, 0.47f, 0.52f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.92f, 0.78f, -0.55f, -1f, -0.35f));
    }

    @Override
    public void render() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glClearColor(0.39f, 0.58f, 0.72f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        cameraController.getTarget(cameraFocus);
        worldState.updatePlayerPosition(cameraFocus.x, cameraFocus.z,
                Math.min(Gdx.graphics.getDeltaTime(), 0.05f));
        updateTileResources(cameraFocus);
        localEnvironment.update(camera, cameraFocus);
        localCreatures.update(camera, cameraFocus, tileX, tileZ);
        waterTime += Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        waterInstance.transform.setToTranslation(
                WorldCoordinates.tileCenter(tileX) + MathUtils.sin(waterTime * 0.18f) * 6f,
                MathUtils.sin(waterTime * 0.42f) * 0.12f,
                WorldCoordinates.tileCenter(tileZ) + MathUtils.cos(waterTime * 0.16f) * 5f);
        if (neighborWaterInstance != null) {
            neighborWaterInstance.transform.setToTranslation(
                    WorldCoordinates.tileCenter(neighborTileX)
                            + MathUtils.sin(waterTime * 0.18f) * 6f,
                    MathUtils.sin(waterTime * 0.42f) * 0.12f,
                    WorldCoordinates.tileCenter(neighborTileZ)
                            + MathUtils.cos(waterTime * 0.16f) * 5f);
        }

        modelBatch.begin(camera);
        if (neighborTerrainInstance != null) {
            modelBatch.render(neighborTerrainInstance, environment);
        }
        modelBatch.render(terrainInstance, environment);
        if (neighborWaterInstance != null) {
            modelBatch.render(neighborWaterInstance, environment);
        }
        modelBatch.render(waterInstance, environment);
        modelBatch.render(localEnvironment.cache(), environment);
        modelBatch.render(localCreatures.cache(), environment);
        modelBatch.end();
        worldMinimap.render(worldState.playerX(), worldState.playerZ());
        coordinateOverlay.render(camera, worldState.playerX(), worldState.playerZ());
    }

    @Override
    public void resize(int width, int height) {
        cameraController.updateViewport(width, height);
        worldMinimap.resize(width, height);
        coordinateOverlay.resize(width, height);
    }

    @Override
    public void pause() {
        worldState.flush();
    }

    @Override
    public void dispose() {
        worldState.flush();
        modelBatch.dispose();
        terrainModel.dispose();
        if (neighborTerrainModel != null) {
            neighborTerrainModel.dispose();
        }
        waterModel.dispose();
        localEnvironment.dispose();
        localCreatures.dispose();
        worldMinimap.dispose();
        coordinateOverlay.dispose();
    }

    private Model createTerrainModel(int terrainTileX, int terrainTileZ) {
        float tileCenterX = WorldCoordinates.tileCenter(terrainTileX);
        float tileCenterZ = WorldCoordinates.tileCenter(terrainTileZ);
        int vertexCount = (GRID_CELLS + 1) * (GRID_CELLS + 1);
        float[] vertices = new float[vertexCount * FLOATS_PER_VERTEX];
        short[] indices = new short[GRID_CELLS * GRID_CELLS * 6];
        float spacing = TERRAIN_SIZE / GRID_CELLS;
        float halfSize = TERRAIN_SIZE * 0.5f;
        Vector3 normal = new Vector3();
        int rowSize = GRID_CELLS + 1;
        float[] heights = new float[vertexCount];

        for (int zIndex = 0; zIndex <= GRID_CELLS; zIndex++) {
            float z = zIndex * spacing - halfSize;
            for (int xIndex = 0; xIndex <= GRID_CELLS; xIndex++) {
                float x = xIndex * spacing - halfSize;
                heights[zIndex * rowSize + xIndex] = WorldGenerator.height(
                        tileCenterX + x, tileCenterZ + z);
            }
        }

        int vertexOffset = 0;
        for (int zIndex = 0; zIndex <= GRID_CELLS; zIndex++) {
            float z = zIndex * spacing - halfSize;
            for (int xIndex = 0; xIndex <= GRID_CELLS; xIndex++) {
                float x = xIndex * spacing - halfSize;
                int vertexIndex = zIndex * rowSize + xIndex;
                float y = heights[vertexIndex];
                float left = heights[zIndex * rowSize + Math.max(0, xIndex - 1)];
                float right = heights[zIndex * rowSize + Math.min(GRID_CELLS, xIndex + 1)];
                float down = heights[Math.max(0, zIndex - 1) * rowSize + xIndex];
                float up = heights[Math.min(GRID_CELLS, zIndex + 1) * rowSize + xIndex];
                normal.set(left - right, spacing * 2f, down - up).nor();

                vertices[vertexOffset++] = x;
                vertices[vertexOffset++] = y;
                vertices[vertexOffset++] = z;
                vertices[vertexOffset++] = normal.x;
                vertices[vertexOffset++] = normal.y;
                vertices[vertexOffset++] = normal.z;
                vertices[vertexOffset++] = WorldGenerator.terrainColor(
                        tileCenterX + x, y, tileCenterZ + z, normal.x, normal.y, normal.z);
            }
        }

        int indexOffset = 0;
        for (int z = 0; z < GRID_CELLS; z++) {
            for (int x = 0; x < GRID_CELLS; x++) {
                short bottomLeft = (short) (z * rowSize + x);
                short bottomRight = (short) (bottomLeft + 1);
                short topLeft = (short) (bottomLeft + rowSize);
                short topRight = (short) (topLeft + 1);

                indices[indexOffset++] = bottomLeft;
                indices[indexOffset++] = topLeft;
                indices[indexOffset++] = topRight;
                indices[indexOffset++] = bottomLeft;
                indices[indexOffset++] = topRight;
                indices[indexOffset++] = bottomRight;
            }
        }

        Mesh mesh = new Mesh(true, vertexCount, indices.length,
                VertexAttribute.Position(),
                VertexAttribute.Normal(),
                VertexAttribute.ColorPacked());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        Material material = new Material(ColorAttribute.createDiffuse(Color.WHITE));
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("terrain", mesh, GL20.GL_TRIANGLES, material);
        return modelBuilder.end();
    }


    private Model createWaterModel() {
        float halfSize = TERRAIN_SIZE * 0.5f + 16f;
        int rowSize = WATER_GRID_CELLS + 1;
        int vertexCount = rowSize * rowSize;
        float[] vertices = new float[vertexCount * 6];
        short[] indices = new short[WATER_GRID_CELLS * WATER_GRID_CELLS * 6];
        float spacing = halfSize * 2f / WATER_GRID_CELLS;
        float waterHeight = WorldGenerator.WATER_LEVEL + 0.35f;
        Vector3 normal = new Vector3();
        int vertexOffset = 0;
        for (int zIndex = 0; zIndex <= WATER_GRID_CELLS; zIndex++) {
            float z = zIndex * spacing - halfSize;
            for (int xIndex = 0; xIndex <= WATER_GRID_CELLS; xIndex++) {
                float x = xIndex * spacing - halfSize;
                float waveX = x * 0.012f;
                float waveZ = z * 0.009f;
                float y = waterHeight + MathUtils.sin(waveX) * 0.55f
                        + MathUtils.cos(waveZ) * 0.38f;
                float slopeX = MathUtils.cos(waveX) * 0.0066f;
                float slopeZ = -MathUtils.sin(waveZ) * 0.00342f;
                normal.set(-slopeX, 1f, -slopeZ).nor();
                vertices[vertexOffset++] = x;
                vertices[vertexOffset++] = y;
                vertices[vertexOffset++] = z;
                vertices[vertexOffset++] = normal.x;
                vertices[vertexOffset++] = normal.y;
                vertices[vertexOffset++] = normal.z;
            }
        }

        int indexOffset = 0;
        for (int zIndex = 0; zIndex < WATER_GRID_CELLS; zIndex++) {
            for (int xIndex = 0; xIndex < WATER_GRID_CELLS; xIndex++) {
                short bottomLeft = (short) (zIndex * rowSize + xIndex);
                short bottomRight = (short) (bottomLeft + 1);
                short topLeft = (short) (bottomLeft + rowSize);
                short topRight = (short) (topLeft + 1);
                indices[indexOffset++] = bottomLeft;
                indices[indexOffset++] = topLeft;
                indices[indexOffset++] = topRight;
                indices[indexOffset++] = bottomLeft;
                indices[indexOffset++] = topRight;
                indices[indexOffset++] = bottomRight;
            }
        }
        Mesh mesh = new Mesh(true, vertexCount, indices.length,
                VertexAttribute.Position(),
                VertexAttribute.Normal());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        Material waterMaterial = new Material(
                ColorAttribute.createDiffuse(new Color(0.09f, 0.32f, 0.43f, 1f)),
                ColorAttribute.createSpecular(new Color(0.62f, 0.76f, 0.82f, 1f)),
                FloatAttribute.createShininess(30f),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.66f));
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("water", mesh, GL20.GL_TRIANGLES, waterMaterial);
        return modelBuilder.end();
    }

    private void updateTileResources(Vector3 focus) {
        int focusTileX = WorldCoordinates.tileIndex(focus.x);
        int focusTileZ = WorldCoordinates.tileIndex(focus.z);
        if (focusTileX != tileX || focusTileZ != tileZ) {
            int previousTileX = tileX;
            int previousTileZ = tileZ;
            Model previousTerrainModel = terrainModel;
            ModelInstance previousTerrainInstance = terrainInstance;
            if (neighborTerrainModel != null
                    && neighborTileX == focusTileX && neighborTileZ == focusTileZ) {
                terrainModel = neighborTerrainModel;
                terrainInstance = neighborTerrainInstance;
                neighborTerrainModel = previousTerrainModel;
                neighborTerrainInstance = previousTerrainInstance;
                neighborTileX = previousTileX;
                neighborTileZ = previousTileZ;
            } else {
                Model loadedTerrainModel = createTerrainModel(focusTileX, focusTileZ);
                ModelInstance loadedTerrainInstance = new ModelInstance(loadedTerrainModel);
                loadedTerrainInstance.transform.setToTranslation(
                        WorldCoordinates.tileCenter(focusTileX), 0f,
                        WorldCoordinates.tileCenter(focusTileZ));
                disposeNeighborTile();
                terrainModel = loadedTerrainModel;
                terrainInstance = loadedTerrainInstance;
                neighborTerrainModel = previousTerrainModel;
                neighborTerrainInstance = previousTerrainInstance;
                neighborWaterInstance = new ModelInstance(waterModel);
                neighborTileX = previousTileX;
                neighborTileZ = previousTileZ;
            }
            tileX = focusTileX;
            tileZ = focusTileZ;
            localEnvironment.setTile(tileX, tileZ);
        }

        float localX = WorldCoordinates.localCoordinate(focus.x, tileX);
        float localZ = WorldCoordinates.localCoordinate(focus.z, tileZ);
        boolean nearXEdge = Math.abs(localX)
                >= WorldCoordinates.HALF_TILE_SIZE - WorldCoordinates.EDGE_APPROACH_DISTANCE;
        boolean nearZEdge = Math.abs(localZ)
                >= WorldCoordinates.HALF_TILE_SIZE - WorldCoordinates.EDGE_APPROACH_DISTANCE;
        if (!nearXEdge && !nearZEdge) {
            disposeNeighborTile();
            return;
        }

        int desiredTileX = tileX;
        int desiredTileZ = tileZ;
        if (nearXEdge && (!nearZEdge || Math.abs(localX) >= Math.abs(localZ))) {
            desiredTileX += localX >= 0f ? 1 : -1;
        } else {
            desiredTileZ += localZ >= 0f ? 1 : -1;
        }
        if (neighborTerrainModel == null
                || neighborTileX != desiredTileX || neighborTileZ != desiredTileZ) {
            loadNeighborTile(desiredTileX, desiredTileZ);
        }
    }

    private void loadNeighborTile(int newNeighborTileX, int newNeighborTileZ) {
        Model loadedTerrainModel = createTerrainModel(newNeighborTileX, newNeighborTileZ);
        ModelInstance loadedTerrainInstance = new ModelInstance(loadedTerrainModel);
        loadedTerrainInstance.transform.setToTranslation(
                WorldCoordinates.tileCenter(newNeighborTileX), 0f,
                WorldCoordinates.tileCenter(newNeighborTileZ));
        disposeNeighborTile();
        neighborTerrainModel = loadedTerrainModel;
        neighborTerrainInstance = loadedTerrainInstance;
        neighborWaterInstance = new ModelInstance(waterModel);
        neighborTileX = newNeighborTileX;
        neighborTileZ = newNeighborTileZ;
    }

    private void disposeNeighborTile() {
        if (neighborTerrainModel != null) {
            neighborTerrainModel.dispose();
            neighborTerrainModel = null;
            neighborTerrainInstance = null;
            neighborWaterInstance = null;
        }
        neighborTileX = Integer.MIN_VALUE;
        neighborTileZ = Integer.MIN_VALUE;
    }

    static float terrainSurfaceHeight(float x, float z) {
        float spacing = TERRAIN_SIZE / GRID_CELLS;
        int surfaceTileX = WorldCoordinates.tileIndex(x);
        int surfaceTileZ = WorldCoordinates.tileIndex(z);
        float originX = WorldCoordinates.tileCenter(surfaceTileX)
                - WorldCoordinates.HALF_TILE_SIZE;
        float originZ = WorldCoordinates.tileCenter(surfaceTileZ)
                - WorldCoordinates.HALF_TILE_SIZE;
        float gridX = MathUtils.clamp((x - originX) / spacing, 0f, GRID_CELLS);
        float gridZ = MathUtils.clamp((z - originZ) / spacing, 0f, GRID_CELLS);
        int cellX = Math.min(GRID_CELLS - 1, MathUtils.floor(gridX));
        int cellZ = Math.min(GRID_CELLS - 1, MathUtils.floor(gridZ));
        float fractionX = gridX - cellX;
        float fractionZ = gridZ - cellZ;
        float x0 = originX + cellX * spacing;
        float z0 = originZ + cellZ * spacing;
        float bottomLeft = WorldGenerator.height(x0, z0);
        float bottomRight = WorldGenerator.height(x0 + spacing, z0);
        float topLeft = WorldGenerator.height(x0, z0 + spacing);
        float topRight = WorldGenerator.height(x0 + spacing, z0 + spacing);
        if (fractionZ >= fractionX) {
            return bottomLeft + fractionZ * (topLeft - bottomLeft)
                    + fractionX * (topRight - topLeft);
        }
        return bottomLeft + fractionX * (bottomRight - bottomLeft)
                + fractionZ * (topRight - bottomRight);
    }

    static float terrainHeight(float x, float z) {
        return WorldGenerator.height(x, z);
    }
}
