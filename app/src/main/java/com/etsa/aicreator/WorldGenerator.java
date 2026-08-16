package com.etsa.aicreator;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

/** Deterministic local-world sampling shared by terrain, camera, and environment placement. */
final class WorldGenerator {
    static final long WORLD_SEED = 0x5EED5A17L;
    static final float WATER_LEVEL = 4f;

    private WorldGenerator() {
    }

    static float height(float x, float z) {
        float broad = fbm(x * 0.00055f, z * 0.00055f, 4, 0x11);
        float lowlands = fbm(x * 0.0011f + 23f, z * 0.0011f - 17f, 3, 0x29);
        float hills = fbm(x * 0.0031f, z * 0.0031f, 4, 0x37);
        float region = fbm(x * 0.00082f - 31f, z * 0.00082f + 19f, 4, 0x53);
        float ridgeSource = fbm(x * 0.00175f + 41f, z * 0.00175f + 7f, 4, 0x71);
        float ridges = 1f - Math.abs(ridgeSource);
        ridges = (float) Math.pow(MathUtils.clamp(ridges, 0f, 1f), 1.75f);

        float mountainMask = smoothstep(0.08f, 0.62f, region);
        float plainMask = smoothstep(0.30f, 0.72f,
                fbm(x * 0.00135f + 73f, z * 0.00135f - 51f, 3, 0x97));

        float rollingGround = broad * 34f + lowlands * 19f;
        float localRelief = hills * MathUtils.lerp(22f, 6f, plainMask);
        float mountains = mountainMask * (34f + ridges * 148f);
        float cliffBands = mountainMask * (float) Math.pow(ridges, 4.8f) * 78f;
        return -13f + rollingGround + localRelief + mountains + cliffBands;
    }

    static float moisture(float x, float z) {
        return fbm(x * 0.00125f - 11f, z * 0.00125f + 67f, 4, 0xB3);
    }

    static float forestDensity(float x, float z) {
        float grouping = fbm(x * 0.0017f + 101f, z * 0.0017f - 83f, 4, 0xD1);
        float moisture = moisture(x, z);
        return smoothstep(0.05f, 0.58f, grouping * 0.72f + moisture * 0.45f);
    }

    static boolean isSwamp(float x, float z, float height) {
        return height > WATER_LEVEL - 1.5f
                && height < WATER_LEVEL + 14f
                && moisture(x, z) > 0.24f;
    }

    static float slope(float x, float z) {
        float sample = 8f;
        float dx = height(x + sample, z) - height(x - sample, z);
        float dz = height(x, z + sample) - height(x, z - sample);
        return MathUtils.clamp((float) Math.sqrt(dx * dx + dz * dz) / (sample * 2f), 0f, 2f);
    }

    static void normal(Vector3 result, float x, float z, float sampleDistance) {
        float left = height(x - sampleDistance, z);
        float right = height(x + sampleDistance, z);
        float down = height(x, z - sampleDistance);
        float up = height(x, z + sampleDistance);
        result.set(left - right, sampleDistance * 2f, down - up).nor();
    }

    static float terrainColor(float x, float y, float z, float normalY) {
        float variation = fbm(x * 0.018f, z * 0.018f, 2, 0xE7) * 0.055f;
        float moisture = moisture(x, z);
        float steepness = 1f - normalY;

        float r;
        float g;
        float b;
        if (y < WATER_LEVEL + 3.5f) {
            r = 0.30f;
            g = 0.34f;
            b = 0.25f;
        } else if (isSwamp(x, z, y)) {
            r = 0.22f;
            g = 0.31f;
            b = 0.20f;
        } else if (steepness > 0.24f || y > 112f) {
            float rockLight = MathUtils.clamp((y - 45f) / 205f, 0f, 1f);
            float cliffShade = MathUtils.clamp((steepness - 0.20f) / 0.48f, 0f, 1f) * 0.07f;
            r = MathUtils.lerp(0.32f, 0.56f, rockLight) - cliffShade;
            g = MathUtils.lerp(0.31f, 0.54f, rockLight) - cliffShade;
            b = MathUtils.lerp(0.29f, 0.51f, rockLight) - cliffShade;
        } else if (moisture < -0.25f) {
            r = 0.43f;
            g = 0.40f;
            b = 0.25f;
        } else {
            r = MathUtils.lerp(0.30f, 0.19f, MathUtils.clamp(moisture + 0.3f, 0f, 1f));
            g = MathUtils.lerp(0.43f, 0.38f, MathUtils.clamp(moisture + 0.3f, 0f, 1f));
            b = MathUtils.lerp(0.20f, 0.16f, MathUtils.clamp(moisture + 0.3f, 0f, 1f));
        }

        return com.badlogic.gdx.graphics.Color.toFloatBits(
                MathUtils.clamp(r + variation, 0f, 1f),
                MathUtils.clamp(g + variation, 0f, 1f),
                MathUtils.clamp(b + variation * 0.65f, 0f, 1f),
                1f);
    }

    static float random01(int x, int z, int salt) {
        long value = WORLD_SEED;
        value ^= (long) x * 0x9E3779B97F4A7C15L;
        value ^= (long) z * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) salt * 0x165667B19E3779F9L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value & 0xFFFFFFL) / 16777215f;
    }

    private static float fbm(float x, float z, int octaves, int salt) {
        float value = 0f;
        float amplitude = 0.55f;
        float totalAmplitude = 0f;
        for (int octave = 0; octave < octaves; octave++) {
            value += valueNoise(x, z, salt + octave * 101) * amplitude;
            totalAmplitude += amplitude;
            x *= 2.03f;
            z *= 2.03f;
            amplitude *= 0.5f;
        }
        return value / totalAmplitude;
    }

    private static float valueNoise(float x, float z, int salt) {
        int x0 = MathUtils.floor(x);
        int z0 = MathUtils.floor(z);
        float tx = x - x0;
        float tz = z - z0;
        tx = tx * tx * (3f - 2f * tx);
        tz = tz * tz * (3f - 2f * tz);

        float a = random01(x0, z0, salt) * 2f - 1f;
        float b = random01(x0 + 1, z0, salt) * 2f - 1f;
        float c = random01(x0, z0 + 1, salt) * 2f - 1f;
        float d = random01(x0 + 1, z0 + 1, salt) * 2f - 1f;
        return MathUtils.lerp(MathUtils.lerp(a, b, tx), MathUtils.lerp(c, d, tx), tz);
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = MathUtils.clamp((value - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
