package template;

public class Tuple<X, Y> {

    private final X x;
    private final Y y;

    public Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public Y getY() {
        return y;
    }

    public X getX() {
        return x;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Tuple)) {
            return false;
        }
        Tuple c = (Tuple) o;
        return this.x.equals(c.x) && this.y.equals(c.y);
    }

    @Override
    public int hashCode() {
        return x.hashCode() * (y.hashCode() + 256);
    }

}