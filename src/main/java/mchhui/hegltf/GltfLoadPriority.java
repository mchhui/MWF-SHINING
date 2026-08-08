package mchhui.hegltf;

public enum GltfLoadPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2);

    public final int rank;

    GltfLoadPriority(int rank) {
        this.rank = rank;
    }

    public GltfLoadPriority max(GltfLoadPriority other) {
        return other != null && other.rank > this.rank ? other : this;
    }
}
