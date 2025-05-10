package map.gen;

import godot.core.Vector2;
import godot.core.Vector2i;
import godot.core.Vector3;
import godot.core.Vector3i;
import java.util.Objects;

public class Coordinate {

    private double x;
    private double z;

    private int xIndex;
    private int zIndex;

    public Coordinate(double x, double z, int xi, int zi) {
        this.x = x;
        this.z = z;
        this.xIndex = xi;
        this.zIndex = zi;
    }

    public Coordinate(double x, int xi) {
        this.x = x;
        this.z = x;
        this.xIndex = xi;
        this.zIndex = xi;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public Vector3 toVec3() {
        return new Vector3(x, 0, z);
    }

    public Vector2 toVec2() {
        return new Vector2(x, z);
    }

    public int getXIndex() {
        return xIndex;
    }

    public int getZIndex() {
        return zIndex;
    }

    public void setXIndex(int xi) {
        this.xIndex = xi;
    }

    public void setZIndex(int zi) {
        this.zIndex = zi;
    }

    public Vector3i toVec3i() {
        return new Vector3i(xIndex, 0, zIndex);
    }

    public Vector2i toVec2i() {
        return new Vector2i(xIndex, zIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GridCell)) return false;
        Coordinate gc = (Coordinate) obj;
        if (gc.getX() != this.getX() || gc.getZ() != this.getZ()) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    @Override
    public String toString() {
        return ("(" + x + ", " + z + " | " + xIndex + ", " + zIndex + ")");
    }
}
