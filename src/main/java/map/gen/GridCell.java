package map.gen;

/**
 * The class GridCell
 * Provides information relating to a grid cell in a world
 * Has getter and setter methods to determine information of a grid cell
 * Also has equals and toString method for gridCell
 */
public class GridCell {

    private Coordinate center;
    private Tile tile;
    private double height;

    /**
     * The constructor for GridCell
     * @param coord the coordinate for the grid cell
     * @param type the type of tile in grid cell
     * @param height the height of the object in grid cell
     */
    public GridCell(Coordinate coord, Tile type, double height) {
        this.center = coord;
        this.tile = type;
        this.height = height;
    }

    /**
     * Getter method for the coordinate
     * @return The center coordinate of the grid cell
     */
    public Coordinate getCoords() {
        return center;
    }

    /**
     * Getter method for the tile type
     * @return the tile type
     */
    public Tile getTile() {
        return tile;
    }

    /**
     * Getter method for the height
     * @return the height of the grid cell
     */
    public double getHeight() {
        return height;
    }

    /**
     * Setter method for the tile type
     * @param tile The tile type to be set
     */
    public void setTile(Tile tile) {
        this.tile = tile;
    }

    /**
     * Setter method for the tile height
     * @param height the height to be set
     */
    public void setHeight(double height) {
        this.height = height;
    }

    /**
     * Checks if two tiles are equal
     * @param obj The tile to compare to
     * @return If the two tiles are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GridCell)) return false;
        GridCell gc = (GridCell) obj;
        if (
            gc.getTile() != this.getTile() || gc.getCoords() != this.getCoords()
        ) return false;
        return true;
    }

    /**
     * Converts the tile to a string form
     * @return A string version of the grid
     */
    @Override
    public String toString() {
        return center.toString();
    }
}
