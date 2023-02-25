package org.eclipse.milo.platform.compression;

public class SwingDoor {
    private final double tolerance;
    private long startX;
    private double startY;
    private long lastX;
    private double lastY;
    private double minSlope;
    private double maxSlope;
    private int count = 0;

    public SwingDoor(double tolerance) {
        this.tolerance = tolerance;
    }

    public boolean isAccepted(long time, double value) {
        count++;
        // just store the first value
        if (count == 1) {
            startX = time;
            startY = value;
            return true;
        } else if (count == 2) {
            // setting the min and max slope for the first time
            minSlope = ((value - tolerance) - startY) / (time - startX);
            maxSlope = ((value + tolerance) - startY) / (time - startX);
            return true;
        } else {
            double currentMinSlope = ((value - tolerance) - startY) / (time - startX);
            double currentMaxSlope = ((value + tolerance) - startY) / (time - startX);
            if (currentMaxSlope < maxSlope) maxSlope = currentMaxSlope;
            if (currentMinSlope > minSlope) minSlope = currentMinSlope;
            // the line reaching from start point and current point, this line's slope should be between min and max slope
            double pointSlope = (value - startY) / (time - startX);
            if (isOutOfDoor(pointSlope)) {
                startX = lastX;
                startY = lastY;
                lastX = time;
                lastY = value;
                minSlope = ((value - tolerance) - startY) / (time - startX);
                maxSlope = ((value + tolerance) - startY) / (time - startX);
                return true;
            }
            return false;
        }

    }

    private boolean isOutOfDoor(double pointSlope) {
        return pointSlope > maxSlope || pointSlope < minSlope;
    }

}
