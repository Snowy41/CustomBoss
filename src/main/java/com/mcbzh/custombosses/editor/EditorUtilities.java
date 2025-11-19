package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for editor operations
 */
public class EditorUtilities {

    /**
     * Snap value to grid
     */
    public static double snap(double value, double gridSize) {
        if (gridSize == 0) return value;
        return Math.round(value / gridSize) * gridSize;
    }

    public static Vector snapVector(Vector v, double gridSize) {
        return new Vector(
                snap(v.getX(), gridSize),
                snap(v.getY(), gridSize),
                snap(v.getZ(), gridSize)
        );
    }

    /**
     * Clone part data
     */
    public static ModelData.PartData clonePartData(ModelData.PartData original) {
        ModelData.PartData clone = new ModelData.PartData(original.id + "_copy");
        clone.parentId = original.parentId;
        clone.material = original.material;
        clone.position = original.position.clone();
        clone.rotation = original.rotation.clone();
        clone.scale = original.scale.clone();
        return clone;
    }

    /**
     * Mirror part across axis
     */
    public static ModelData.PartData mirrorPart(ModelData.PartData original, Axis axis) {
        ModelData.PartData mirrored = clonePartData(original);
        mirrored.id = original.id + "_mirror";

        switch (axis) {
            case X -> {
                mirrored.position.setX(-mirrored.position.getX());
                mirrored.rotation.setY(-mirrored.rotation.getY());
                mirrored.rotation.setZ(-mirrored.rotation.getZ());
            }
            case Y -> {
                mirrored.position.setY(-mirrored.position.getY());
                mirrored.rotation.setX(-mirrored.rotation.getX());
                mirrored.rotation.setZ(-mirrored.rotation.getZ());
            }
            case Z -> {
                mirrored.position.setZ(-mirrored.position.getZ());
                mirrored.rotation.setX(-mirrored.rotation.getX());
                mirrored.rotation.setY(-mirrored.rotation.getY());
            }
        }

        return mirrored;
    }

    public enum Axis {
        X, Y, Z
    }

    /**
     * Calculate bounding box of entire model
     */
    public static BoundingBox calculateModelBounds(ModelData model) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (ModelData.PartData part : model.getParts()) {
            Vector pos = part.position;
            Vector scale = part.scale;

            minX = Math.min(minX, pos.getX() - scale.getX() / 2);
            minY = Math.min(minY, pos.getY() - scale.getY() / 2);
            minZ = Math.min(minZ, pos.getZ() - scale.getZ() / 2);

            maxX = Math.max(maxX, pos.getX() + scale.getX() / 2);
            maxY = Math.max(maxY, pos.getY() + scale.getY() / 2);
            maxZ = Math.max(maxZ, pos.getZ() + scale.getZ() / 2);
        }

        return new BoundingBox(
                new Vector(minX, minY, minZ),
                new Vector(maxX, maxY, maxZ)
        );
    }

    public static class BoundingBox {
        public Vector min;
        public Vector max;

        public BoundingBox(Vector min, Vector max) {
            this.min = min;
            this.max = max;
        }

        public Vector getSize() {
            return max.clone().subtract(min);
        }

        public Vector getCenter() {
            return min.clone().add(max).multiply(0.5);
        }
    }

    /**
     * Convert euler angles to quaternion
     */
    public static Quaternionf eulerToQuaternion(Vector euler) {
        return new Quaternionf().rotateXYZ(
                (float) Math.toRadians(euler.getX()),
                (float) Math.toRadians(euler.getY()),
                (float) Math.toRadians(euler.getZ())
        );
    }

    /**
     * Convert quaternion to euler angles
     */
    public static Vector quaternionToEuler(Quaternionf q) {
        // Calculate euler angles from quaternion
        float x = q.x();
        float y = q.y();
        float z = q.z();
        float w = q.w();

        // Roll (x-axis)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        double roll = Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (y-axis)
        double sinp = 2 * (w * y - z * x);
        double pitch;
        if (Math.abs(sinp) >= 1) {
            pitch = Math.copySign(Math.PI / 2, sinp);
        } else {
            pitch = Math.asin(sinp);
        }

        // Yaw (z-axis)
        double siny_cosp = 2 * (w * z + x * y);
        double cosy_cosp = 1 - 2 * (y * y + z * z);
        double yaw = Math.atan2(siny_cosp, cosy_cosp);

        return new Vector(
                Math.toDegrees(roll),
                Math.toDegrees(pitch),
                Math.toDegrees(yaw)
        );
    }

    /**
     * Transform point by matrix
     */
    public static Vector transformPoint(Vector point, Matrix4f matrix) {
        Vector3f v = new Vector3f(
                (float) point.getX(),
                (float) point.getY(),
                (float) point.getZ()
        );

        matrix.transformPosition(v);

        return new Vector(v.x(), v.y(), v.z());
    }

    /**
     * Get transform matrix for a part (including parent hierarchy)
     */
    public static Matrix4f getGlobalMatrix(ModelData.PartData part, ModelData model) {
        Matrix4f matrix = new Matrix4f();

        // Build hierarchy chain
        List<ModelData.PartData> chain = new ArrayList<>();
        ModelData.PartData current = part;
        while (current != null) {
            chain.add(0, current); // Add to front
            current = current.parentId != null ? model.getPart(current.parentId) : null;
        }

        // Apply transforms in order
        for (ModelData.PartData p : chain) {
            matrix.translate(
                    (float) p.position.getX(),
                    (float) p.position.getY(),
                    (float) p.position.getZ()
            );

            matrix.rotateXYZ(
                    (float) Math.toRadians(p.rotation.getX()),
                    (float) Math.toRadians(p.rotation.getY()),
                    (float) Math.toRadians(p.rotation.getZ())
            );

            matrix.scale(
                    (float) p.scale.getX(),
                    (float) p.scale.getY(),
                    (float) p.scale.getZ()
            );
        }

        return matrix;
    }

    /**
     * Calculate distance from point to line segment
     */
    public static double distanceToLineSegment(Vector point, Vector lineStart, Vector lineEnd) {
        Vector line = lineEnd.clone().subtract(lineStart);
        double lineLength = line.length();

        if (lineLength == 0) {
            return point.distance(lineStart);
        }

        Vector toPoint = point.clone().subtract(lineStart);
        double t = Math.max(0, Math.min(1, toPoint.dot(line) / (lineLength * lineLength)));

        Vector projection = lineStart.clone().add(line.multiply(t));
        return point.distance(projection);
    }

    /**
     * Raycast to find BlockDisplay intersection
     */
    public static boolean rayIntersectsBox(Location rayOrigin, Vector rayDirection,
                                           Location boxCenter, Vector boxSize) {
        // AABB ray intersection test
        Vector min = boxCenter.toVector().subtract(boxSize.clone().multiply(0.5));
        Vector max = boxCenter.toVector().add(boxSize.clone().multiply(0.5));

        Vector rayOriginV = rayOrigin.toVector();

        double tmin = (min.getX() - rayOriginV.getX()) / rayDirection.getX();
        double tmax = (max.getX() - rayOriginV.getX()) / rayDirection.getX();

        if (tmin > tmax) {
            double temp = tmin;
            tmin = tmax;
            tmax = temp;
        }

        double tymin = (min.getY() - rayOriginV.getY()) / rayDirection.getY();
        double tymax = (max.getY() - rayOriginV.getY()) / rayDirection.getY();

        if (tymin > tymax) {
            double temp = tymin;
            tymin = tymax;
            tymax = temp;
        }

        if ((tmin > tymax) || (tymin > tmax)) return false;

        if (tymin > tmin) tmin = tymin;
        if (tymax < tmax) tmax = tymax;

        double tzmin = (min.getZ() - rayOriginV.getZ()) / rayDirection.getZ();
        double tzmax = (max.getZ() - rayOriginV.getZ()) / rayDirection.getZ();

        if (tzmin > tzmax) {
            double temp = tzmin;
            tzmin = tzmax;
            tzmax = temp;
        }

        if ((tmin > tzmax) || (tzmin > tmax)) return false;

        return true;
    }

    /**
     * Get material palette for quick access
     */
    public static Material[] getCommonPalette() {
        return new Material[] {
                // Concrete
                Material.WHITE_CONCRETE,
                Material.LIGHT_GRAY_CONCRETE,
                Material.GRAY_CONCRETE,
                Material.BLACK_CONCRETE,
                Material.RED_CONCRETE,
                Material.ORANGE_CONCRETE,
                Material.YELLOW_CONCRETE,
                Material.LIME_CONCRETE,
                Material.GREEN_CONCRETE,
                Material.CYAN_CONCRETE,
                Material.LIGHT_BLUE_CONCRETE,
                Material.BLUE_CONCRETE,
                Material.PURPLE_CONCRETE,
                Material.MAGENTA_CONCRETE,
                Material.PINK_CONCRETE,
                Material.BROWN_CONCRETE,

                // Glazed Terracotta (detailed)
                Material.WHITE_GLAZED_TERRACOTTA,
                Material.RED_GLAZED_TERRACOTTA,
                Material.CYAN_GLAZED_TERRACOTTA,
                Material.LIGHT_BLUE_GLAZED_TERRACOTTA,

                // Metals
                Material.IRON_BLOCK,
                Material.GOLD_BLOCK,
                Material.DIAMOND_BLOCK,
                Material.EMERALD_BLOCK,
                Material.NETHERITE_BLOCK,

                // Organic
                Material.OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.STONE,
                Material.SMOOTH_STONE,
                Material.COBBLESTONE,

                // Special
                Material.GLASS,
                Material.GLOWSTONE,
                Material.SEA_LANTERN,
                Material.REDSTONE_LAMP
        };
    }

    /**
     * Format vector for display
     */
    public static String formatVector(Vector v) {
        return String.format("%.2f, %.2f, %.2f", v.getX(), v.getY(), v.getZ());
    }

    /**
     * Parse vector from string
     */
    public static Vector parseVector(String str) {
        String[] parts = str.replace(" ", "").split(",");
        if (parts.length != 3) return null;

        try {
            return new Vector(
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validate part name
     */
    public static boolean isValidPartName(String name) {
        if (name == null || name.isEmpty()) return false;
        if (name.length() > 32) return false;
        return name.matches("[a-zA-Z0-9_]+");
    }

    /**
     * Generate unique part ID
     */
    public static String generatePartId(String prefix) {
        return prefix + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}