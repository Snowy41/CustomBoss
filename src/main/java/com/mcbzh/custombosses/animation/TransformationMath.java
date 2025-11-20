package com.mcbzh.custombosses.animation;

import com.mcbzh.custombosses.model.ModelData;
import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * CRITICAL FIX: Proper hierarchical transformation calculation
 * This fixes the fundamental issue where parent-child relationships weren't
 * correctly transforming child positions and rotations
 */
public class TransformationMath {

    /**
     * Calculate the global transformation for a part including all parent transforms
     * This properly stacks transformations from root to child
     */
    public static Transformation calculateGlobalTransform(ModelInstance.Part part, ModelInstance instance) {
        // Build the chain from child to root
        List<ModelInstance.Part> chain = buildParentChain(part, instance);

        // Start with identity matrix
        Matrix4f finalMatrix = new Matrix4f().identity();

        // Apply transformations from root to child (proper order!)
        for (ModelInstance.Part p : chain) {
            ModelData.PartData data = p.getData();

            // Create local transformation matrix
            Matrix4f localMatrix = new Matrix4f().identity();

            // 1. Translate to position
            localMatrix.translate(
                    (float) data.position.getX(),
                    (float) data.position.getY(),
                    (float) data.position.getZ()
            );

            // 2. Apply rotation (in XYZ order)
            localMatrix.rotateXYZ(
                    (float) Math.toRadians(data.rotation.getX()),
                    (float) Math.toRadians(data.rotation.getY()),
                    (float) Math.toRadians(data.rotation.getZ())
            );

            // 3. Apply scale
            localMatrix.scale(
                    (float) data.scale.getX(),
                    (float) data.scale.getY(),
                    (float) data.scale.getZ()
            );

            // Multiply: finalMatrix = finalMatrix * localMatrix
            finalMatrix.mul(localMatrix);
        }

        // Extract components from final matrix
        Vector3f translation = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        Vector3f scale = new Vector3f();

        finalMatrix.getTranslation(translation);
        finalMatrix.getUnnormalizedRotation(rotation);
        finalMatrix.getScale(scale);

        // Create Bukkit Transformation
        return new Transformation(
                translation,
                rotation,
                scale,
                new Quaternionf() // Left rotation (unused)
        );
    }

    /**
     * Build parent chain from child to root
     * Returns list in ROOT -> CHILD order
     */
    private static List<ModelInstance.Part> buildParentChain(ModelInstance.Part part, ModelInstance instance) {
        List<ModelInstance.Part> chain = new ArrayList<>();
        ModelInstance.Part current = part;

        // Build chain backwards (child to root)
        while (current != null) {
            chain.add(0, current); // Add to front
            String parentId = current.getData().parentId;
            current = parentId != null ? instance.getParts().get(parentId) : null;
        }

        return chain;
    }

    /**
     * Calculate world position of a part
     */
    public static Vector getGlobalPosition(ModelInstance.Part part, ModelInstance instance, Vector rootLocation) {
        Transformation transform = calculateGlobalTransform(part, instance);
        Vector3f pos = transform.getTranslation();
        return rootLocation.clone().add(new Vector(pos.x(), pos.y(), pos.z()));
    }

    /**
     * Calculate world rotation of a part
     */
    public static Vector getGlobalRotation(ModelInstance.Part part, ModelInstance instance) {
        Transformation transform = calculateGlobalTransform(part, instance);
        Quaternionf quat = transform.getLeftRotation();
        return quaternionToEuler(quat);
    }

    /**
     * Convert quaternion to euler angles (degrees)
     */
    public static Vector quaternionToEuler(Quaternionf q) {
        // Extract euler angles from quaternion
        float x = q.x();
        float y = q.y();
        float z = q.z();
        float w = q.w();

        // Roll (X-axis rotation)
        double sinr_cosp = 2 * (w * x + y * z);
        double cosr_cosp = 1 - 2 * (x * x + y * y);
        double roll = Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (Y-axis rotation)
        double sinp = 2 * (w * y - z * x);
        double pitch;
        if (Math.abs(sinp) >= 1) {
            pitch = Math.copySign(Math.PI / 2, sinp);
        } else {
            pitch = Math.asin(sinp);
        }

        // Yaw (Z-axis rotation)
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
     * Convert euler angles (degrees) to quaternion
     */
    public static Quaternionf eulerToQuaternion(Vector euler) {
        return new Quaternionf().rotateXYZ(
                (float) Math.toRadians(euler.getX()),
                (float) Math.toRadians(euler.getY()),
                (float) Math.toRadians(euler.getZ())
        );
    }

    /**
     * Interpolate between two transformations (for animation blending)
     */
    public static Transformation lerp(Transformation a, Transformation b, float t) {
        // Lerp translation
        Vector3f trans = new Vector3f(
                a.getTranslation().x() + (b.getTranslation().x() - a.getTranslation().x()) * t,
                a.getTranslation().y() + (b.getTranslation().y() - a.getTranslation().y()) * t,
                a.getTranslation().z() + (b.getTranslation().z() - a.getTranslation().z()) * t
        );

        // Slerp rotation (spherical interpolation)
        Quaternionf rot = new Quaternionf();
        a.getLeftRotation().slerp(b.getLeftRotation(), t, rot);

        // Lerp scale
        Vector3f scale = new Vector3f(
                a.getScale().x() + (b.getScale().x() - a.getScale().x()) * t,
                a.getScale().y() + (b.getScale().y() - a.getScale().y()) * t,
                a.getScale().z() + (b.getScale().z() - a.getScale().z()) * t
        );

        return new Transformation(trans, rot, scale, new Quaternionf());
    }

    /**
     * Check if part hierarchy has circular dependencies
     */
    public static boolean hasCircularDependency(ModelData model) {
        for (ModelData.PartData part : model.getParts()) {
            Set<String> visited = new HashSet<>();
            ModelData.PartData current = part;

            while (current != null) {
                if (!visited.add(current.id)) {
                    return true; // Circular dependency found
                }

                String parentId = current.parentId;
                current = parentId != null ? model.getPart(parentId) : null;
            }
        }
        return false;
    }
}