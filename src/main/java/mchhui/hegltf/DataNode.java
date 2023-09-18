package mchhui.hegltf;

import de.javagl.jgltf.model.NodeModel;
import org.joml.*;

import java.util.ArrayList;
import java.util.HashMap;

public class DataNode {
    private static final String PARENT = "###SCENE###";

    public NodeModel unsafeNode;

    public String name;
    public String parent = PARENT;
    public Vector3f pos = new Vector3f(0, 0, 0);
    public Quaternionf rot = new Quaternionf(0, 0, 0, 1);
    public Vector3f size = new Vector3f(1, 1, 1);

    public ArrayList<String> childlist = new ArrayList<String>();

    //key:material name
    public HashMap<String, DataMesh> meshes = new HashMap<String, DataMesh>();

    public boolean isRootNode() {
        return parent == PARENT;
    }
}
