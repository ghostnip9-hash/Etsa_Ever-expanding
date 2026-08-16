package com.etsa.aicreator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/** Lightweight overview of the deterministic world; detailed geometry stays local. */
final class WorldMinimap {
    private static final float WORLD_SIZE = 24_000f;
    private static final float LOCAL_VIEW_SIZE = 6_000f;
    private static final int MAP_SAMPLES = 96;
    private static final float MARGIN = 18f;
    private static final float MAX_SCREEN_FRACTION = 0.28f;
    private static final float MAX_SIZE = 220f;
    private static final float MIN_SIZE = 112f;
    private final SpriteBatch batch = new SpriteBatch();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Matrix4 projection = new Matrix4();
    private final Texture worldTexture;
    private float size;
    private float left;
    private float bottom;

    WorldMinimap() {
        Pixmap map = new Pixmap(MAP_SAMPLES, MAP_SAMPLES, Pixmap.Format.RGBA8888);
        float sampleSpacing = WORLD_SIZE / MAP_SAMPLES;
        float halfWorld = WORLD_SIZE * 0.5f;
        for (int mapZ = 0; mapZ < MAP_SAMPLES; mapZ++) {
            float worldZ = (mapZ + 0.5f) * sampleSpacing - halfWorld;
            for (int mapX = 0; mapX < MAP_SAMPLES; mapX++) {
                float worldX = (mapX + 0.5f) * sampleSpacing - halfWorld;
                float height = WorldGenerator.height(worldX, worldZ);
                float moisture = (WorldGenerator.moisture(worldX, worldZ) + 1f) * 0.5f;
                setMapColor(map, mapX, MAP_SAMPLES - 1 - mapZ, height, moisture);
            }
        }
        worldTexture = new Texture(map);
        worldTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        map.dispose();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    void resize(int width, int height) {
        projection.setToOrtho2D(0f, 0f, Math.max(1, width), Math.max(1, height));
        size = MathUtils.clamp(Math.min(width, height) * MAX_SCREEN_FRACTION, MIN_SIZE, MAX_SIZE);
        left = width - size - MARGIN;
        bottom = MARGIN;
    }

    void render(Vector3 position) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(projection);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.025f, 0.035f, 0.04f, 0.86f);
        shapes.rect(left - 5f, bottom - 5f, size + 10f, size + 10f);
        shapes.end();
        batch.setProjectionMatrix(projection);
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.94f);
        batch.draw(worldTexture, left, bottom, size, size);
        batch.end();

        float localSize = size * LOCAL_VIEW_SIZE / WORLD_SIZE;
        float localLeft = left + (size - localSize) * 0.5f;
        float localBottom = bottom + (size - localSize) * 0.5f;
        float markerX = left + MathUtils.clamp(position.x / WORLD_SIZE + 0.5f, 0f, 1f) * size;
        float markerY = bottom + MathUtils.clamp(position.z / WORLD_SIZE + 0.5f, 0f, 1f) * size;
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.88f, 0.92f, 0.82f, 0.72f);
        shapes.rect(left, bottom, size, size);
        shapes.setColor(1f, 1f, 1f, 0.52f);
        shapes.rect(localLeft, localBottom, localSize, localSize);
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(1f, 0.72f, 0.12f, 1f);
        shapes.circle(markerX, markerY, 4.5f, 16);
        shapes.setColor(1f, 1f, 0.82f, 1f);
        shapes.circle(markerX, markerY, 1.8f, 10);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    void dispose() {
        worldTexture.dispose();
        batch.dispose();
        shapes.dispose();
    }

    private static void setMapColor(Pixmap map, int x, int y, float height, float moisture) {
        Color color;
        if (height <= WorldGenerator.WATER_LEVEL) {
            float depth = MathUtils.clamp((WorldGenerator.WATER_LEVEL - height) / 55f, 0f, 1f);
            color = new Color(MathUtils.lerp(0.10f, 0.035f, depth),
                    MathUtils.lerp(0.36f, 0.16f, depth), MathUtils.lerp(0.47f, 0.29f, depth), 1f);
        } else if (height > 120f) {
            float peak = MathUtils.clamp((height - 120f) / 150f, 0f, 1f);
            color = new Color(MathUtils.lerp(0.43f, 0.72f, peak),
                    MathUtils.lerp(0.42f, 0.70f, peak), MathUtils.lerp(0.39f, 0.67f, peak), 1f);
        } else if (height < WorldGenerator.WATER_LEVEL + 8f) {
            color = new Color(0.55f, 0.49f, 0.31f, 1f);
        } else {
            color = new Color(MathUtils.lerp(0.35f, 0.15f, moisture),
                    MathUtils.lerp(0.39f, 0.43f, moisture), MathUtils.lerp(0.20f, 0.13f, moisture), 1f);
        }
        map.drawPixel(x, y, Color.rgba8888(color));
    }
}
