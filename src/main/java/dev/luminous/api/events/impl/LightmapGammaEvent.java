package dev.luminous.api.events.impl;

import dev.luminous.api.events.Event;

public class LightmapGammaEvent extends Event {
   private float gamma;

   public LightmapGammaEvent(float gamma) {
      this.gamma = gamma;
   }

   public float getGamma() {
      return this.gamma;
   }

   public void setGamma(int gamma) {
      this.gamma = gamma;
   }
}
