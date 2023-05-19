package com.modularwarfare.client.fpp.enhanced.models;

import com.modularmods.mcgltf.RenderedGltfScene;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public interface IRenderedGltfModelMWF {

    GltfModel getGltfModel();

    List<RenderedGltfScene> getRenderedGltfScenes();

    List<Pair<NodeModel, MutableBoolean>> getSingleNodeVisibleToggles();
}
