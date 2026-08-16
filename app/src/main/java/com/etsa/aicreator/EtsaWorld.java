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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector3;

/** Seeded local-region renderer for the natural-world milestone. */
public final class EtsaWorld extends ApplicationAdapter {
    private static final int GRID_CELLS = 192;
    private static final float TERRAIN_SIZE = 6_000f;
    private static final int FLOATS_PER_VERTEX = 7;

    private PerspectiveCamera camera;
    private RtsCameraController cameraController;
    private ModelBatch modelBatch;
    private Model terrainModel;
    private Model waterModel;
    private ModelInstance terrainInstance;
    private ModelInstance waterInstance;
    private LocalEnvironment localEnvironment;
    private final Vector3 cameraFocus = new Vector3();
    private Environment environment;

    @Override
    public void create() {
        camera = new PerspectiveCamera(55f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraController = new RtsCameraController(camera);
        Gdx.input.setInputProcessor(new GestureDetector(cameraController));

        modelBatch = new ModelBatch();
        terrainModel = createTerrainModel();
        terrainInstance = new ModelInstance(terrainModel);
        waterModel = createWaterModel();
        waterInstance = new ModelInstance(waterModel);
        localEnvironment = new LocalEnvironment();

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
        localEnvironment.update(camera, cameraFocus);

        modelBatch.begin(camera);
        modelBatch.render(terrainInstance, environment);
        modelBatch.render(waterInstance, environment);
        modelBatch.render(localEnvironment.cache(), environment);
        modelBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        cameraController.updateViewport(width, height);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        terrainModel.dispose();
        waterModel.dispose();
        localEnvironment.dispose();
    }

    private Model createTerrainModel() {
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
                heights[zIndex * rowSize + xIndex] = WorldGenerator.height(x, z);
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
                vertices[vertexOffset++] = WorldGenerator.terrainColor(x, y, z, normal.x, normal.y, normal.z);
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
        float halfSize = TERRAIN_SIZE * 0.5f;
        float waterHeight = WorldGenerator.WATER_LEVEL + 0.35f;
        float[] vertices = {
                -halfSize, waterHeight, -halfSize, 0f, 1f, 0f,
                -halfSize, waterHeight, halfSize, 0f, 1f, 0f,
                halfSize, waterHeight, halfSize, 0f, 1f, 0f,
                halfSize, waterHeight, -halfSize, 0f, 1f, 0f
        };
        short[] indices = {0, 1, 2, 0, 2, 3};
        Mesh mesh = new Mesh(true, 4, 6,
                VertexAttribute.Position(),
                VertexAttribute.Normal());
        mesh.setVertices(vertices);
        mesh.setIndices(indices);

        Material waterMaterial = new Material(
                ColorAttribute.createDiffuse(new Color(0.09f, 0.32f, 0.43f, 1f)),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.68f));
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();
        modelBuilder.part("water", mesh, GL20.GL_TRIANGLES, waterMaterial);
        return modelBuilder.end();
    }

    static float terrainHeight(float x, float z) {
        return WorldGenerator.height(x, z);
    }
}
