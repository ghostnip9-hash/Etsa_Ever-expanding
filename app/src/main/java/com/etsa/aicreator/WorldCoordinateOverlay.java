package com.etsa.aicreator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

/** Displays the persistent world position and future neighboring-tile destination. */
final class WorldCoordinateOverlay {
    private static final float TILE_SIZE = 6_000f;
    private static final float HALF_TILE_SIZE = TILE_SIZE * 0.5f;
    private static final float EDGE_NOTICE_DISTANCE = 250f;
    private static final float MARGIN = 14f;

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout layout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4();
    private final StringBuilder text = new StringBuilder(64);
    private int screenWidth;
    private int screenHeight;
    private int lastWorldX = Integer.MIN_VALUE;
    private int lastWorldZ = Integer.MIN_VALUE;
    private int lastTileX = Integer.MIN_VALUE;
    private int lastTileZ = Integer.MIN_VALUE;
    private int lastNextTileX = Integer.MIN_VALUE;
    private int lastNextTileZ = Integer.MIN_VALUE;
    private boolean lastHasNextTile;

    WorldCoordinateOverlay() {
        font.getData().setScale(0.72f);
        font.setColor(0.94f, 0.96f, 0.90f, 0.92f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    void resize(int width, int height) {
        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);
        projection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
    }

    void render(float worldX, float worldZ) {
        int roundedX = Math.round(worldX);
        int roundedZ = Math.round(worldZ);
        int tileX = MathUtils.floor((worldX + HALF_TILE_SIZE) / TILE_SIZE);
        int tileZ = MathUtils.floor((worldZ + HALF_TILE_SIZE) / TILE_SIZE);
        float localX = worldX - tileX * TILE_SIZE;
        float localZ = worldZ - tileZ * TILE_SIZE;
        int nextTileX = tileX;
        int nextTileZ = tileZ;
        boolean hasNextTile = false;

        if (localX >= HALF_TILE_SIZE - EDGE_NOTICE_DISTANCE) {
            nextTileX++;
            hasNextTile = true;
        } else if (localX <= -HALF_TILE_SIZE + EDGE_NOTICE_DISTANCE) {
            nextTileX--;
            hasNextTile = true;
        }
        if (localZ >= HALF_TILE_SIZE - EDGE_NOTICE_DISTANCE) {
            nextTileZ++;
            hasNextTile = true;
        } else if (localZ <= -HALF_TILE_SIZE + EDGE_NOTICE_DISTANCE) {
            nextTileZ--;
            hasNextTile = true;
        }

        if (roundedX != lastWorldX || roundedZ != lastWorldZ
                || tileX != lastTileX || tileZ != lastTileZ
                || nextTileX != lastNextTileX || nextTileZ != lastNextTileZ
                || hasNextTile != lastHasNextTile) {
            text.setLength(0);
            text.append("X ").append(roundedX).append("  Z ").append(roundedZ)
                    .append("  TILE [").append(tileX).append(',').append(tileZ).append(']');
            if (hasNextTile) {
                text.append("\nNEXT TILE [").append(nextTileX).append(',')
                        .append(nextTileZ).append(']');
            }
            layout.setText(font, text);
            lastWorldX = roundedX;
            lastWorldZ = roundedZ;
            lastTileX = tileX;
            lastTileZ = tileZ;
            lastNextTileX = nextTileX;
            lastNextTileZ = nextTileZ;
            lastHasNextTile = hasNextTile;
        }

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(projection);
        batch.begin();
        font.draw(batch, layout, screenWidth - MARGIN - layout.width,
                screenHeight - MARGIN);
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    void dispose() {
        font.dispose();
        batch.dispose();
    }
}
