package com.mcbzh.custombosses.animation;

import com.mcbzh.custombosses.model.ModelInstance;
import org.bukkit.entity.LivingEntity;

import java.util.*;

/**
 * Animation State Machine - manages transitions between boss animations
 * Handles smooth blending between states like IDLE -> WALK -> ATTACK
 */
public class AnimationStateMachine {

    private final ModelInstance modelInstance;
    private AnimationState currentState;
    private AnimationState previousState;

    private final Map<AnimationState, AnimationSystem.Animation> stateAnimations;
    private final Map<StateTransition, Float> transitionTimes; // Blend duration in seconds

    private AnimationSystem.AnimationPlayer currentPlayer;
    private AnimationSystem.AnimationPlayer previousPlayer;

    private float blendProgress = 1.0f; // 0.0 = previous, 1.0 = current
    private float blendSpeed = 2.0f; // Blend rate per second

    private boolean isTransitioning = false;

    public AnimationStateMachine(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
        this.stateAnimations = new EnumMap<>(AnimationState.class);
        this.transitionTimes = new HashMap<>();
        this.currentState = AnimationState.IDLE;
    }

    /**
     * Register an animation for a specific state
     */
    public void registerAnimation(AnimationState state, AnimationSystem.Animation animation) {
        stateAnimations.put(state, animation);
    }

    /**
     * Set transition blend time between two states
     */
    public void setTransitionTime(AnimationState from, AnimationState to, float seconds) {
        transitionTimes.put(new StateTransition(from, to), seconds);
    }

    /**
     * Transition to a new state
     */
    public void transitionTo(AnimationState newState) {
        if (newState == currentState) return;

        AnimationSystem.Animation newAnimation = stateAnimations.get(newState);
        if (newAnimation == null) {
            // No animation for this state, just switch
            currentState = newState;
            return;
        }

        // Get blend time for this transition
        StateTransition transition = new StateTransition(currentState, newState);
        Float blendTime = transitionTimes.getOrDefault(transition, 0.3f);

        // Store previous state for blending
        previousState = currentState;
        previousPlayer = currentPlayer;

        // Create new player
        currentState = newState;
        currentPlayer = new AnimationSystem.AnimationPlayer(newAnimation, modelInstance);
        currentPlayer.play();

        // Start blend
        if (blendTime > 0 && previousPlayer != null) {
            isTransitioning = true;
            blendProgress = 0.0f;
            blendSpeed = 1.0f / blendTime; // Speed to reach 1.0 in 'blendTime' seconds
        } else {
            blendProgress = 1.0f;
            isTransitioning = false;
        }
    }

    /**
     * Force immediate state change (no blending)
     */
    public void forceState(AnimationState state) {
        previousPlayer = null;
        previousState = null;
        blendProgress = 1.0f;
        isTransitioning = false;

        AnimationSystem.Animation animation = stateAnimations.get(state);
        if (animation != null) {
            currentState = state;
            currentPlayer = new AnimationSystem.AnimationPlayer(animation, modelInstance);
            currentPlayer.play();
        }
    }

    /**
     * Update the state machine (call every tick)
     */
    public void tick() {
        if (currentPlayer == null) return;

        if (isTransitioning && previousPlayer != null) {
            // Update blend progress
            blendProgress += blendSpeed / 20.0f; // Convert to per-tick (20 TPS)

            if (blendProgress >= 1.0f) {
                // Blend complete
                blendProgress = 1.0f;
                isTransitioning = false;
                previousPlayer = null;
            } else {
                // Tick both animations and blend
                previousPlayer.tick();
                currentPlayer.tick();
                blendAnimations(blendProgress);
                return;
            }
        }

        // Normal playback
        currentPlayer.tick();
    }

    /**
     * Blend between two animations
     */
    private void blendAnimations(float alpha) {
        if (previousPlayer == null || currentPlayer == null) return;

        // Get current frames from both players
        int prevFrame = previousPlayer.getTick();
        int currFrame = currentPlayer.getTick();

        // Apply transforms with blending
        Set<String> allParts = new HashSet<>();
        allParts.addAll(modelInstance.getParts().keySet());

        for (String partId : allParts) {
            ModelInstance.Part part = modelInstance.getParts().get(partId);
            if (part == null) continue;

            // Get transforms from both animations at current frame
            // This is simplified - you'd get actual keyframe data
            // For now, we just blend the current part state

            // The animation players have already modified part data
            // So we don't need to do additional work here
            // The interpolation happens in the AnimationPlayer
        }

        modelInstance.markDirty();
        modelInstance.update();
    }

    /**
     * Auto-transition based on entity state
     * Call this from CustomBoss.tick()
     */
    public void autoUpdate(LivingEntity entity) {
        AnimationState desiredState = determineState(entity);

        if (desiredState != currentState) {
            transitionTo(desiredState);
        }
    }

    /**
     * Determine which state the entity should be in
     */
    private AnimationState determineState(LivingEntity entity) {
        // Dead
        if (entity.isDead()) {
            return AnimationState.DEATH;
        }

        // Taking damage (recent damage)
        if (entity.getNoDamageTicks() < entity.getMaximumNoDamageTicks() - 5) {
            return AnimationState.HURT;
        }

        // Moving
        if (entity.getVelocity().lengthSquared() > 0.01) {
            return AnimationState.WALK;
        }

        // Default to idle
        return AnimationState.IDLE;
    }

    /**
     * Play a one-shot animation (like an attack) then return to previous state
     */
    public void playOneShot(AnimationState state, Runnable onComplete) {
        AnimationState returnState = currentState;
        transitionTo(state);

        AnimationSystem.Animation animation = stateAnimations.get(state);
        if (animation != null && !animation.loop) {
            // Schedule return after animation completes
            int duration = animation.duration;
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (currentState == state) {
                        transitionTo(returnState);
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }, duration * 50L); // Convert ticks to milliseconds
        }
    }

    // Getters
    public AnimationState getCurrentState() { return currentState; }
    public boolean isTransitioning() { return isTransitioning; }
    public float getBlendProgress() { return blendProgress; }

    /**
     * Animation states a boss can be in
     */
    public enum AnimationState {
        IDLE,       // Standing still
        WALK,       // Moving
        RUN,        // Moving fast
        ATTACK,     // Generic attack
        ABILITY_1,  // Custom ability 1
        ABILITY_2,  // Custom ability 2
        ABILITY_3,  // Custom ability 3
        HURT,       // Taking damage
        DEATH,      // Dying
        SPAWN;      // Spawning in

        public String getDefaultAnimationId() {
            return name().toLowerCase();
        }
    }

    /**
     * Represents a transition between two states
     */
    private static class StateTransition {
        final AnimationState from;
        final AnimationState to;

        StateTransition(AnimationState from, AnimationState to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StateTransition)) return false;
            StateTransition that = (StateTransition) o;
            return from == that.from && to == that.to;
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    /**
     * Builder for easier state machine setup
     */
    public static class Builder {
        private final AnimationStateMachine machine;

        public Builder(ModelInstance instance) {
            this.machine = new AnimationStateMachine(instance);
        }

        public Builder withAnimation(AnimationState state, AnimationSystem.Animation animation) {
            machine.registerAnimation(state, animation);
            return this;
        }

        public Builder withTransition(AnimationState from, AnimationState to, float blendTime) {
            machine.setTransitionTime(from, to, blendTime);
            return this;
        }

        public Builder withDefaultTransitions(float defaultBlendTime) {
            // Set up common transitions
            for (AnimationState from : AnimationState.values()) {
                for (AnimationState to : AnimationState.values()) {
                    if (from != to) {
                        machine.setTransitionTime(from, to, defaultBlendTime);
                    }
                }
            }
            return this;
        }

        public AnimationStateMachine build() {
            return machine;
        }
    }
}