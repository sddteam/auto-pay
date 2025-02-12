package com.bastion.inc;

import org.opencv.core.Point;

public class MatchResult {
    public final ActionState templateName;
    public final Point location;
    public final double confidence;

    public MatchResult(ActionState templateName, Point location, double confidence){
        this.templateName = templateName;
        this.location = location;
        this.confidence = confidence;
    }
}
