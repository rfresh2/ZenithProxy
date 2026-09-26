package com.zenith.feature.player;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

@Data
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor
public class Input {
    public boolean pressingForward;
    public boolean pressingBack;
    public boolean pressingLeft;
    public boolean pressingRight;
    public boolean jumping;
    public boolean sneaking;
    public boolean sprinting;
    public boolean leftClick;
    public boolean rightClick;
    public Hand hand;
    public ClickTarget clickTarget;
    public boolean clickRequiresRotation = true;

    public static Builder builder() {
        return new Builder();
    }

    public Input(Input in) {
        this(in.pressingForward, in.pressingBack, in.pressingLeft, in.pressingRight, in.jumping, in.sneaking, in.sprinting, in.leftClick, in.rightClick, in.hand, in.clickTarget, in.clickRequiresRotation);
    }

    public void apply(Input in) {
        if (!pressingForward || !pressingBack) {
            this.pressingForward = in.pressingForward;
            this.pressingBack = in.pressingBack;
        }
        if (!in.pressingLeft || !in.pressingRight) {
            this.pressingLeft = in.pressingLeft;
            this.pressingRight = in.pressingRight;
        }
        this.jumping = in.jumping;
        this.sneaking = in.sneaking;
        this.sprinting = in.sprinting;
        if (this.sprinting && (this.pressingBack || this.sneaking)) {
            this.sprinting = false;
        }
        this.leftClick = in.leftClick;
        this.rightClick = in.rightClick;
        this.hand = in.hand;
        this.clickTarget = in.clickTarget;
        this.clickRequiresRotation = in.clickRequiresRotation;
    }

    public void reset() {
        pressingForward = false;
        pressingBack = false;
        pressingLeft = false;
        pressingRight = false;
        jumping = false;
        sneaking = false;
        sprinting = false;
        leftClick = false;
        rightClick = false;
        hand = Hand.MAIN_HAND;
        clickTarget = ClickTarget.Any.INSTANCE;
        clickRequiresRotation = true;
    }

    public String log() {
        String out = "Input:";
        // only log true values
        if (pressingForward) {
            out += " forward";
        }
        if (pressingBack) {
            out += " back";
        }
        if (pressingLeft) {
            out += " left";
        }
        if (pressingRight) {
            out += " right";
        }
        if (jumping) {
            out += " jump";
        }
        if (sneaking) {
            out += " sneak";
        }
        if (sprinting) {
            out += " sprint";
        }


        // but if none are true, log that
        if (out.equals("Input:")) {
            out += " none";
        }
        return out;
    }

    public static final class Builder {
        private boolean pressingForward;
        private boolean pressingBack;
        private boolean pressingLeft;
        private boolean pressingRight;
        private boolean jumping;
        private boolean sneaking;
        private boolean sprinting;
        private boolean leftClick;
        private boolean rightClick;
        private Hand hand = Hand.MAIN_HAND;
        private ClickTarget clickTarget = ClickTarget.Any.INSTANCE;
        private boolean clickRequiresRotation = true;

        private Builder() {}

        public Builder pressingForward(boolean pressingForward) {
            this.pressingForward = pressingForward;
            return this;
        }

        public Builder pressingBack(boolean pressingBack) {
            this.pressingBack = pressingBack;
            return this;
        }

        public Builder pressingLeft(boolean pressingLeft) {
            this.pressingLeft = pressingLeft;
            return this;
        }

        public Builder pressingRight(boolean pressingRight) {
            this.pressingRight = pressingRight;
            return this;
        }

        public Builder jumping(boolean jumping) {
            this.jumping = jumping;
            return this;
        }

        public Builder sneaking(boolean sneaking) {
            this.sneaking = sneaking;
            return this;
        }

        public Builder sprinting(boolean sprinting) {
            this.sprinting = sprinting;
            return this;
        }

        public Builder leftClick(boolean leftClick) {
            this.leftClick = leftClick;
            return this;
        }

        public Builder rightClick(boolean rightClick) {
            this.rightClick = rightClick;
            return this;
        }

        public Builder hand(Hand hand) {
            this.hand = hand;
            return this;
        }

        public Builder clickTarget(ClickTarget clickTarget) {
            this.clickTarget = clickTarget;
            return this;
        }

        public Builder clickRequiresRotation(boolean clickRequiresRotation) {
            this.clickRequiresRotation = clickRequiresRotation;
            return this;
        }

        public Input build() {
            return new Input(pressingForward, pressingBack, pressingLeft, pressingRight, jumping, sneaking, sprinting, leftClick, rightClick, hand, clickTarget, clickRequiresRotation);
        }
    }
}
