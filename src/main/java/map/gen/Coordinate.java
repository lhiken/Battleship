package map.gen;

import godot.core.Vector2;
import godot.core.Vector2i;
import godot.core.Vector3;
import godot.core.Vector3i;
import java.util.Objects;

/**
 * Stores the cordinate of the game
 */
public class Coordinate {

    private double x;
    private double z;

    private int xIndex;
    private int zIndex;

    /**
     * Constructor for x and z cordinate
     * @param x is actual x position
     * @param z is actual z position
     * @param xi is grid position of x
     * @param zi is grid position of z
     */
    public Coordinate(double x, double z, int xi, int zi) {
        this.x = x;
        this.z = z;
        this.xIndex = xi;
        this.zIndex = zi;
    }

    /**
     * Constructor for x cordinate
     * @param x is actual x position
     * @param xi is grid position of x
     */
    public Coordinate(double x, int xi) {
        this.x = x;
        this.z = x;
        this.xIndex = xi;
        this.zIndex = xi;
    }

    /**
     * @return x position
     */
    public double getX() {
        return x;
    }

    /**
     * @return z position
     */
    public double getZ() {
        return z;
    }

    /**
     * Sets x cordinate value to x
     * @param x is value to set x to
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Sets z cordinate value to z
     * @param z is value to set z to
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * @return 3d vector of x and z cordinate, with y at 0
     */
    public Vector3 toVec3() {
        return new Vector3(x, 0, z);
    }

    /**
     * @return 2d vector of x and z cordinate
     */
    public Vector2 toVec2() {
        return new Vector2(x, z);
    }

    /**
     * @return xIndex
     */
    public int getXIndex() {
        return xIndex;
    }

    /**
     * @return zIndex
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * sets xIndex to xi
     * @param xi is value to replace xIndex with
     */
    public void setXIndex(int xi) {
        this.xIndex = xi;
    }

    /**
     * sets zIndex to zi
     * @param zi is value to replace zIndex with
     */
    public void setZIndex(int zi) {
        this.zIndex = zi;
    }

    /**
     * @return 3d vector of xIndex, 0, and zIndex
     */
    public Vector3i toVec3i() {
        return new Vector3i(xIndex, 0, zIndex);
    }

    /**
     * @return 2d vector of xIndex, zIndex
     */
    public Vector2i toVec2i() {
        return new Vector2i(xIndex, zIndex);
    }

    /**
     * Checks if current cordinate is same as obj's cordinate
     * @param obj is the object to compare with
     * @return true or not
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GridCell)) return false;
        Coordinate gc = (Coordinate) obj;
        if (gc.getX() != this.getX() || gc.getZ() != this.getZ()) return false;
        return true;
    }

    /**
     * @return hashcode of cordinate
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }

    /**
     * @return cordinate as a string
     */
    @Override
    public String toString() {
        return ("(" + x + ", " + z + " | " + xIndex + ", " + zIndex + ")");
    }
}
