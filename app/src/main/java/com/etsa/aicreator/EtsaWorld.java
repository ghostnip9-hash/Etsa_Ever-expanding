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
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** First rendering milestone: one lightweight, temporary terrain mesh. */
public final class EtsaWorld extends ApplicationAdapter {
    private static final int GRID_CELLS = 128;
    private static final float TERRAIN_SIZE = 1_600f;
    private static final int FLOATS_PER_VERTEX = 7;
    private static final float LOW_TERRAIN_COLOR = new Color(0.47f, 0.58f, 0.36f, 1f).toFloatBits();
    private static final float MID_TERRAIN_COLOR = new Color(0.38f, 0.48f, 0.31f, 1f).toFloatBits();
    private static final float HIGH_TERRAIN_COLOR = new Color(0.53f, 0.56f, 0.54f, 1f).toFloatBits();

    private PerspectiveCamera camera;
    private RtsCameraController cameraController;
    private ModelBatch modelBatch;
    private Model terrainModel;
    private ModelInstance terrainInstance;
    private Environment environment;

    @Override
    public void create() {
        camera = new PerspectiveCamera(55f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cameraController = new RtsCameraController(camera);
        Gdx.input.setInputProcessor(new GestureDetector(cameraController));

        modelBatch = new ModelBatch();
        terrainModel = createTerrainModel();
        terrainInstance = new ModelInstance(terrainModel);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.52f, 0.56f, 0.62f, 1f));
        environment.add(new DirectionalLight().set(1f, 0.94f, 0.82f, -0.55f, -1f, -0.35f));
    }

    @Override
    public void render() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_CULL_FACE);
        Gdx.gl.glCullFace(GL20.GL_BACK);
        Gdx.gl.glClearColor(0.39f, 0.58f, 0.72f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(terrainInstance, environment);
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
    }

    private Model createTerrainModel() {
        int vertexCount = (GRID_CELLS + 1) * (GRID_CELLS + 1);
        float[] vertices = new float[vertexCount * FLOATS_PER_VERTEX];
        short[] indices = new short[GRID_CELLS * GRID_CELLS * 6];
        float spacing = TERRAIN_SIZE / GRID_CELLS;
        float halfSize = TERRAIN_SIZE * 0.5f;
        Vector3 normal = new Vector3();

        int vertexOffset = 0;
        for (int zIndex = 0; zIndex <= GRID_CELLS; zIndex++) {
            float z = zIndex * spacing - halfSize;
            for (int xIndex = 0; xIndex <= GRID_CELLS; xIndex++) {
                float x = xIndex * spacing - halfSize;
                float y = terrainHeight(x, z);
                calculateNormal(normal, x, z, spacing);

                vertices[vertexOffset++] = x;
                vertices[vertexOffset++] = y;
                vertices[vertexOffset++] = z;
                vertices[vertexOffset++] = normal.x;
                vertices[vertexOffset++] = normal.y;
                vertices[vertexOffset++] = normal.z;
                vertices[vertexOffset++] = terrainColor(y);
            }
        }

        int indexOffset = 0;
        int rowSize = GRID_CELLS + 1;
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
        return new ModelBuilder().createFromMesh(mesh, GL20.GL_TRIANGLES, material);
    }

    private static void calculateNormal(Vector3 result, float x, float z, float spacing) {
        float left = terrainHeight(x - spacing, z);
        float right = terrainHeight(x + spacing, z);
        float down = terrainHeight(x, z - spacing);
        float up = terrainHeight(x, z + spacing);
        result.set(left - right, spacing * 2f, down - up).nor();
    }

    private static float terrainColor(float height) {
        if (height > 52f) {
            return HIGH_TERRAIN_COLOR;
        }
        if (height > 24f) {
            return MID_TERRAIN_COLOR;
        }
        return LOW_TERRAIN_COLOR;
    }

    static float terrainHeight(float x, float z) {
        float rolling = MathUtils.sin(x * 0.012f) * 13f + MathUtils.cos(z * 0.014f) * 11f;
        float crossed = MathUtils.sin((x + z) * 0.006f) * 18f;
        float ridge = Math.abs(MathUtils.sin(x * 0.0042f) * MathUtils.cos(z * 0.0051f)) * 54f;
        float centralRise = 44f * (float) Math.exp(-(x * x + z * z) / 210_000f);
        return rolling + crossed + ridge + centralRise;
    }
}
