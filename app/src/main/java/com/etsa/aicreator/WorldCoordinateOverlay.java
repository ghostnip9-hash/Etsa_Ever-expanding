package com.etsa.aicreator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/** Displays the persistent world position and future neighboring-tile destination. */
final class WorldCoordinateOverlay {
    private static final float TILE_SIZE = 6_000f;
    private static final float HALF_TILE_SIZE = TILE_SIZE * 0.5f;
    private static final float EDGE_NOTICE_DISTANCE = 250f;
    private static final float BEYOND_EDGE_DISTANCE = 35f;
    private static final float EDGE_END_MARGIN = 200f;
    private static final float MARGIN = 14f;

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout coordinateLayout = new GlyphLayout();
    private final GlyphLayout nextTileLayout = new GlyphLayout();
    private final Matrix4 projection = new Matrix4();
    private final Vector3 groundAnchor = new Vector3();
    private final StringBuilder coordinateText = new StringBuilder(48);
    private final StringBuilder nextTileText = new StringBuilder(24);
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
        font.getData().setScale(1f);
        font.setColor(0f, 0f, 0f, 1f);
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    void resize(int width, int height) {
        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);
        projection.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
    }

    void render(PerspectiveCamera camera, float worldX, float worldZ) {
        int roundedX = Math.round(worldX);
        int roundedZ = Math.round(worldZ);
        int tileX = MathUtils.floor((worldX + HALF_TILE_SIZE) / TILE_SIZE);
        int tileZ = MathUtils.floor((worldZ + HALF_TILE_SIZE) / TILE_SIZE);
        float localX = worldX - tileX * TILE_SIZE;
        float localZ = worldZ - tileZ * TILE_SIZE;
        int nextTileX = tileX;
        int nextTileZ = tileZ;
        boolean nearXEdge = Math.abs(localX) >= HALF_TILE_SIZE - EDGE_NOTICE_DISTANCE;
        boolean nearZEdge = Math.abs(localZ) >= HALF_TILE_SIZE - EDGE_NOTICE_DISTANCE;
        boolean hasNextTile = nearXEdge || nearZEdge;
        boolean useXEdge = nearXEdge && (!nearZEdge || Math.abs(localX) >= Math.abs(localZ));

        if (hasNextTile) {
            if (useXEdge) {
                nextTileX += localX >= 0f ? 1 : -1;
            } else {
                nextTileZ += localZ >= 0f ? 1 : -1;
            }
        }

        if (roundedX != lastWorldX || roundedZ != lastWorldZ
                || tileX != lastTileX || tileZ != lastTileZ
                || nextTileX != lastNextTileX || nextTileZ != lastNextTileZ
                || hasNextTile != lastHasNextTile) {
            coordinateText.setLength(0);
            coordinateText.append("X ").append(roundedX).append("  Z ").append(roundedZ)
                    .append("  TILE [").append(tileX).append(',').append(tileZ).append(']');
            coordinateLayout.setText(font, coordinateText);
            if (hasNextTile) {
                nextTileText.setLength(0);
                nextTileText.append("NEXT TILE [").append(nextTileX).append(',')
                        .append(nextTileZ).append(']');
                nextTileLayout.setText(font, nextTileText);
            }
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
        font.draw(batch, coordinateLayout, screenWidth - MARGIN - coordinateLayout.width,
                screenHeight - MARGIN);
        if (hasNextTile) {
            setGroundAnchor(useXEdge, localX, localZ);
            camera.project(groundAnchor, 0f, 0f, screenWidth, screenHeight);
            if (groundAnchor.z >= 0f && groundAnchor.z <= 1f
                    && groundAnchor.x >= 0f && groundAnchor.x <= screenWidth
                    && groundAnchor.y >= 0f && groundAnchor.y <= screenHeight) {
                font.draw(batch, nextTileLayout, groundAnchor.x - nextTileLayout.width * 0.5f,
                        groundAnchor.y + nextTileLayout.height * 0.5f);
            }
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    void dispose() {
        font.dispose();
        batch.dispose();
    }

    private void setGroundAnchor(boolean useXEdge, float localX, float localZ) {
        float anchorX;
        float anchorZ;
        if (useXEdge) {
            anchorX = Math.copySign(HALF_TILE_SIZE + BEYOND_EDGE_DISTANCE, localX);
            anchorZ = MathUtils.clamp(localZ, -HALF_TILE_SIZE + EDGE_END_MARGIN,
                    HALF_TILE_SIZE - EDGE_END_MARGIN);
        } else {
            anchorX = MathUtils.clamp(localX, -HALF_TILE_SIZE + EDGE_END_MARGIN,
                    HALF_TILE_SIZE - EDGE_END_MARGIN);
            anchorZ = Math.copySign(HALF_TILE_SIZE + BEYOND_EDGE_DISTANCE, localZ);
        }
        float terrainX = MathUtils.clamp(anchorX, -HALF_TILE_SIZE, HALF_TILE_SIZE);
        float terrainZ = MathUtils.clamp(anchorZ, -HALF_TILE_SIZE, HALF_TILE_SIZE);
        groundAnchor.set(anchorX, EtsaWorld.terrainSurfaceHeight(terrainX, terrainZ) + 1f, anchorZ);
    }
}
