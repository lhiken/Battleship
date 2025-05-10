package map.gen;

public class GridCell {

    private Coordinate center;
    private Tile tile;
    private double height;

    public GridCell(Coordinate coord, Tile type, double height) {
        this.center = coord;
        this.tile = type;
        this.height = height;
    }

    public Coordinate getCoords() {
        return center;
    }

    public Tile getTile() {
        return tile;
    }

    public double getHeight() {
        return height;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof GridCell)) return false;
        GridCell gc = (GridCell) obj;
        if (
            gc.getTile() != this.getTile() || gc.getCoords() != this.getCoords()
        ) return false;
        return true;
    }

    @Override
    public String toString() {
        return center.toString();
    }
}
