package com.etsa.aicreator;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

/** Touch controller for a fixed-angle, terrain-safe RTS camera. */
final class RtsCameraController extends GestureDetector.GestureAdapter {
    private static final float PITCH_DEGREES = 36f;
    private static final float MIN_DISTANCE = 90f;
    private static final float MAX_DISTANCE = 1_800f;
    private static final float ROTATION_DEGREES_PER_PIXEL = 0.18f;

    private final PerspectiveCamera camera;
    private final Vector3 target = new Vector3(0f, EtsaWorld.terrainHeight(0f, 0f), 0f);
    private final Vector3 groundRight = new Vector3();
    private final Vector3 groundForward = new Vector3();
    private float distance = 430f;
    private float zoomStartDistance;
    private float yawDegrees = 45f;
    private float rotationStartYaw;
    private boolean zooming;
    private boolean rotating;

    RtsCameraController(PerspectiveCamera camera) {
        this.camera = camera;
        updateCamera();
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        float worldUnitsPerPixel = distance / Math.max(1f, camera.viewportHeight) * 1.35f;
        groundRight.set(camera.direction).crs(Vector3.Y).nor();
        groundForward.set(camera.direction.x, 0f, camera.direction.z).nor();
        target.mulAdd(groundRight, -deltaX * worldUnitsPerPixel);
        target.mulAdd(groundForward, deltaY * worldUnitsPerPixel);
        updateTargetHeight();
        updateCamera();
        return true;
    }

    @Override
    public boolean zoom(float initialDistance, float currentDistance) {
        if (currentDistance <= 0f) {
            return false;
        }
        if (!zooming) {
            zoomStartDistance = distance;
            zooming = true;
        }
        distance = MathUtils.clamp(zoomStartDistance * initialDistance / currentDistance,
                MIN_DISTANCE, MAX_DISTANCE);
        updateCamera();
        return true;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
                         Vector2 pointer1, Vector2 pointer2) {
        if (!rotating) {
            rotationStartYaw = yawDegrees;
            rotating = true;
        }
        float initialMidpointX = (initialPointer1.x + initialPointer2.x) * 0.5f;
        float currentMidpointX = (pointer1.x + pointer2.x) * 0.5f;
        yawDegrees = (rotationStartYaw
                + (currentMidpointX - initialMidpointX) * ROTATION_DEGREES_PER_PIXEL) % 360f;
        updateCamera();
        return true;
    }

    @Override
    public void pinchStop() {
        zooming = false;
        rotating = false;
    }

    void getTarget(Vector3 result) {
        result.set(target);
    }

    void setTarget(float worldX, float worldZ) {
        target.set(worldX, 0f, worldZ);
        updateTargetHeight();
        updateCamera();
    }

    void updateViewport(int width, int height) {
        camera.viewportWidth = Math.max(1, width);
        camera.viewportHeight = Math.max(1, height);
        updateCamera();
    }

    private void updateTargetHeight() {
        target.y = EtsaWorld.terrainHeight(target.x, target.z);
    }

    private void updateCamera() {
        float horizontalDistance = MathUtils.cosDeg(PITCH_DEGREES) * distance;
        float verticalDistance = MathUtils.sinDeg(PITCH_DEGREES) * distance;
        target.y = EtsaWorld.terrainHeight(target.x, target.z);
        camera.position.set(
                target.x + horizontalDistance * MathUtils.cosDeg(yawDegrees),
                target.y + Math.max(35f, verticalDistance),
                target.z + horizontalDistance * MathUtils.sinDeg(yawDegrees));
        float minimumSafeHeight = EtsaWorld.terrainHeight(camera.position.x, camera.position.z) + 24f;
        camera.position.y = Math.max(camera.position.y, minimumSafeHeight);
        camera.lookAt(target);
        camera.up.set(Vector3.Y);
        camera.near = 1f;
        camera.far = 3_800f;
        camera.update();
    }
}
