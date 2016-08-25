package com.optimizely.ab.event.internal.payloadV2;

public class LayerState {

    private String layerId;
    private Decision decision;
    private boolean actionTriggered;

    public LayerState() { }

    public LayerState(String layerId, Decision decision, boolean actionTriggered) {
        this.layerId = layerId;
        this.decision = decision;
        this.actionTriggered = actionTriggered;
    }

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }

    public boolean getActionTriggered() {
        return actionTriggered;
    }

    public void setActionTriggered(boolean actionTriggered) {
        this.actionTriggered = actionTriggered;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof LayerState))
            return false;

        LayerState otherLayerState = (LayerState)other;

        return layerId.equals(otherLayerState.getLayerId()) &&
               decision.equals(otherLayerState.getDecision()) &&
               actionTriggered == otherLayerState.getActionTriggered();
    }

    @Override
    public int hashCode() {
        int result = layerId != null ? layerId.hashCode() : 0;
        result = 31 * result + (decision != null ? decision.hashCode() : 0);
        result = 31 * result + (actionTriggered ? 1 : 0);
        return result;
    }
}
