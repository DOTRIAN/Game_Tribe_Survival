package map;

import javafx.scene.image.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiledMapLoader {

    // Load map TMX va resolve luon tileset / object layer.
    public MapData load(String tmxPath) throws Exception {
        File tmxFile = new File(tmxPath);
        Document tmxDoc = parseXml(tmxFile);
        Element mapElement = tmxDoc.getDocumentElement();

        int mapWidth = parseInt(mapElement.getAttribute("width"), 0);
        int mapHeight = parseInt(mapElement.getAttribute("height"), 0);
        int tileWidth = parseInt(mapElement.getAttribute("tilewidth"), 16);
        int tileHeight = parseInt(mapElement.getAttribute("tileheight"), 16);

        List<TilesetData> tilesets = readTilesets(mapElement, tmxFile.getParentFile());
        List<TileLayerData> layers = readTileLayers(mapElement, mapWidth, mapHeight);
        List<MapObjectData> collisions = readCollisionObjects(mapElement, tmxFile.getParentFile());

        return new MapData(mapWidth, mapHeight, tileWidth, tileHeight, layers, collisions, tilesets);
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private List<TilesetData> readTilesets(Element mapElement, File mapDirectory) throws Exception {
        List<TilesetData> tilesets = new ArrayList<>();
        NodeList tilesetNodes = mapElement.getElementsByTagName("tileset");
        for (int i = 0; i < tilesetNodes.getLength(); i++) {
            Element tilesetRef = (Element) tilesetNodes.item(i);
            int firstGid = parseInt(tilesetRef.getAttribute("firstgid"), 0);
            String source = tilesetRef.getAttribute("source");
            Element tsxRoot;
            File baseDir;
            if (source == null || source.isEmpty()) {
                // Ho tro map nhung tileset inline (khong tach .tsx).
                tsxRoot = tilesetRef;
                baseDir = mapDirectory;
            } else {
                File tsxFile = new File(mapDirectory, source);
                Document tsxDoc = parseXml(tsxFile);
                tsxRoot = tsxDoc.getDocumentElement();
                baseDir = tsxFile.getParentFile();
            }

            int tileWidth = parseInt(tsxRoot.getAttribute("tilewidth"), 16);
            int tileHeight = parseInt(tsxRoot.getAttribute("tileheight"), 16);

            Element imageElement = (Element) tsxRoot.getElementsByTagName("image").item(0);
            if (imageElement == null) {
                continue;
            }
            String imageSource = imageElement.getAttribute("source");
            File imageFile = new File(baseDir, imageSource);
            Image tilesetImage = new Image(imageFile.toURI().toString());

            int columns = parseInt(tsxRoot.getAttribute("columns"), 0);
            if (columns <= 0 && tileWidth > 0 && tilesetImage.getWidth() > 0) {
                columns = Math.max(1, (int) (tilesetImage.getWidth() / tileWidth));
            }
            if (columns <= 0) {
                columns = 1;
            }

            int tileCount = parseInt(tsxRoot.getAttribute("tilecount"), 0);
            if (tileCount <= 0 && tileWidth > 0 && tileHeight > 0 && tilesetImage.getWidth() > 0 && tilesetImage.getHeight() > 0) {
                int rows = Math.max(1, (int) (tilesetImage.getHeight() / tileHeight));
                tileCount = Math.max(1, columns * rows);
            }

            Map<Integer, TilesetData.TileAnimationData> animations = readTileAnimations(tsxRoot);
            Map<Integer, Map<String, String>> tileProperties = readTilesetTileProperties(tsxRoot);

            tilesets.add(new TilesetData(
                    firstGid,
                    columns,
                    tileWidth,
                    tileHeight,
                    tileCount,
                    tilesetImage,
                    animations,
                    tileProperties
            ));
        }
        return tilesets;
    }

    // Doc properties cua tung tile trong TSX.
    // Muc tieu: ho tro gameplay adapter khi map khong dung Object Layer.
    private Map<Integer, Map<String, String>> readTilesetTileProperties(Element tsxRoot) {
        Map<Integer, Map<String, String>> allProperties = new HashMap<>();
        NodeList tileNodes = tsxRoot.getElementsByTagName("tile");
        for (int i = 0; i < tileNodes.getLength(); i++) {
            Node tileNode = tileNodes.item(i);
            if (!(tileNode instanceof Element)) {
                continue;
            }
            Element tileElement = (Element) tileNode;
            int tileId = parseInt(tileElement.getAttribute("id"), -1);
            if (tileId < 0) {
                continue;
            }

            Map<String, String> properties = readProperties(tileElement);
            if (!properties.isEmpty()) {
                allProperties.put(tileId, properties);
            }
        }
        return allProperties;
    }

    private Map<Integer, TilesetData.TileAnimationData> readTileAnimations(Element tsxRoot) {
        Map<Integer, TilesetData.TileAnimationData> animations = new HashMap<>();
        NodeList tileNodes = tsxRoot.getElementsByTagName("tile");
        for (int i = 0; i < tileNodes.getLength(); i++) {
            Node tileNode = tileNodes.item(i);
            if (!(tileNode instanceof Element)) {
                continue;
            }
            Element tileElement = (Element) tileNode;
            int tileId = parseInt(tileElement.getAttribute("id"), -1);
            if (tileId < 0) {
                continue;
            }

            NodeList animationNodes = tileElement.getElementsByTagName("animation");
            if (animationNodes.getLength() == 0) {
                continue;
            }
            Element animationElement = (Element) animationNodes.item(0);
            NodeList frameNodes = animationElement.getElementsByTagName("frame");
            if (frameNodes.getLength() == 0) {
                continue;
            }

            int[] frameIds = new int[frameNodes.getLength()];
            long[] frameDurations = new long[frameNodes.getLength()];
            for (int j = 0; j < frameNodes.getLength(); j++) {
                Node frameNode = frameNodes.item(j);
                if (!(frameNode instanceof Element)) {
                    frameIds[j] = tileId;
                    frameDurations[j] = 120_000_000L;
                    continue;
                }
                Element frameElement = (Element) frameNode;
                frameIds[j] = parseInt(frameElement.getAttribute("tileid"), tileId);
                long durationMs = parseInt(frameElement.getAttribute("duration"), 120);
                frameDurations[j] = Math.max(durationMs, 1L) * 1_000_000L;
            }

            animations.put(tileId, new TilesetData.TileAnimationData(frameIds, frameDurations));
        }
        return animations;
    }

    private List<TileLayerData> readTileLayers(Element mapElement, int mapWidth, int mapHeight) {
        List<TileLayerData> layers = new ArrayList<>();
        NodeList layerNodes = mapElement.getElementsByTagName("layer");
        for (int i = 0; i < layerNodes.getLength(); i++) {
            Element layer = (Element) layerNodes.item(i);
            String layerName = layer.getAttribute("name");

            Element data = (Element) layer.getElementsByTagName("data").item(0);
            String csv = data.getTextContent().trim();
            int[] gids = parseCsv(csv, mapWidth * mapHeight);

            layers.add(new TileLayerData(layerName, mapWidth, mapHeight, gids));
        }
        return layers;
    }

    private List<MapObjectData> readCollisionObjects(Element mapElement, File mapDirectory) throws Exception {
        List<MapObjectData> collisions = new ArrayList<>();
        NodeList objectGroups = mapElement.getElementsByTagName("objectgroup");

        for (int i = 0; i < objectGroups.getLength(); i++) {
            Element group = (Element) objectGroups.item(i);
            Map<String, String> groupProperties = readProperties(group);
            boolean collisionLayer = isCollisionGroup(group.getAttribute("name"), groupProperties);

            NodeList objectNodes = group.getElementsByTagName("object");
            for (int j = 0; j < objectNodes.getLength(); j++) {
                Element object = (Element) objectNodes.item(j);

                // Template cung cap default cho name/type/size/properties.
                TemplateData templateData = loadTemplateData(object, mapDirectory);

                int id = parseInt(object.getAttribute("id"), -1);
                String name = firstNonEmpty(object.getAttribute("name"), templateData.name);
                String type = firstNonEmpty(object.getAttribute("type"), templateData.type);

                double x = parseDouble(object.getAttribute("x"), 0);
                double y = parseDouble(object.getAttribute("y"), 0);
                double width = parseDouble(object.getAttribute("width"), templateData.width);
                double height = parseDouble(object.getAttribute("height"), templateData.height);

                Map<String, String> properties = new HashMap<>(templateData.properties);
                properties.putAll(readProperties(object));

                boolean objectCollision = collisionLayer || isCollisionMarker(name, type, properties);
                if (!objectCollision && !isBaseCampMarker(name, type, properties)) {
                    continue;
                }

                collisions.add(new MapObjectData(id, name, type, x, y, width, height, properties));
            }
        }
        return collisions;
    }

    private boolean isCollisionGroup(String name, Map<String, String> properties) {
        if (matchesCollisionToken(name)) {
            return true;
        }
        return hasTruthyProperty(properties, "collision", "collisions", "blocked", "solid");
    }

    private boolean isCollisionMarker(String name, String type, Map<String, String> properties) {
        return matchesCollisionToken(name)
                || matchesCollisionToken(type)
                || hasTruthyProperty(properties, "collision", "collisions", "blocked", "solid");
    }

    private TemplateData loadTemplateData(Element objectElement, File mapDirectory) throws Exception {
        String templatePath = objectElement.getAttribute("template");
        if (templatePath == null || templatePath.isEmpty()) {
            return TemplateData.empty();
        }

        File templateFile = new File(mapDirectory, templatePath);
        Document templateDoc = parseXml(templateFile);
        Element templateRoot = templateDoc.getDocumentElement();
        Element templateObject = (Element) templateRoot.getElementsByTagName("object").item(0);

        String name = templateObject.getAttribute("name");
        String type = templateObject.getAttribute("type");
        double width = parseDouble(templateObject.getAttribute("width"), 0);
        double height = parseDouble(templateObject.getAttribute("height"), 0);
        Map<String, String> properties = readProperties(templateObject);

        return new TemplateData(name, type, width, height, properties);
    }

    private Map<String, String> readProperties(Element parentElement) {
        Map<String, String> properties = new HashMap<>();
        NodeList propertiesNodes = parentElement.getElementsByTagName("properties");
        if (propertiesNodes.getLength() == 0) {
            return properties;
        }

        Element propertiesElement = (Element) propertiesNodes.item(0);
        NodeList propertyNodes = propertiesElement.getElementsByTagName("property");
        for (int i = 0; i < propertyNodes.getLength(); i++) {
            Node node = propertyNodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            Element property = (Element) node;
            String key = property.getAttribute("name");
            String value = property.getAttribute("value");
            if ((value == null || value.isEmpty()) && property.getTextContent() != null) {
                value = property.getTextContent().trim();
            }
            properties.put(key, value);
        }
        return properties;
    }

    private int[] parseCsv(String csv, int expectedSize) {
        String[] rawParts = csv.replace("\r", "").replace("\n", "").split(",");
        int[] gids = new int[expectedSize];
        int limit = Math.min(rawParts.length, expectedSize);
        for (int i = 0; i < limit; i++) {
            String part = rawParts[i].trim();
            gids[i] = part.isEmpty() ? 0 : parseInt(part, 0);
        }
        return gids;
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String firstNonEmpty(String current, String fallback) {
        if (current != null && !current.isEmpty()) {
            return current;
        }
        return fallback == null ? "" : fallback;
    }

    private boolean isBaseCampMarker(String name, String type, Map<String, String> properties) {
        if (hasTruthyProperty(properties, "isBaseCamp", "baseCamp", "BaseCamp")) {
            return true;
        }
        return matchesBaseCampToken(name) || matchesBaseCampToken(type);
    }

    private boolean matchesBaseCampToken(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("basecamp")
                || normalized.equals("mainhouse")
                || normalized.equals("main_house")
                || normalized.equals("main-house");
    }

    private boolean matchesCollisionToken(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("collision")
                || normalized.equals("collisions")
                || normalized.equals("collider")
                || normalized.equals("blocker")
                || normalized.equals("blockers")
                || normalized.equals("solid");
    }

    private boolean hasTruthyProperty(Map<String, String> properties, String... keys) {
        for (String key : keys) {
            String raw = properties.get(key);
            if (raw == null) {
                continue;
            }
            String normalized = raw.trim().toLowerCase();
            if (normalized.equals("true") || normalized.equals("on") || normalized.equals("1") || normalized.equals("yes")) {
                return true;
            }
        }
        return false;
    }

    private static final class TemplateData {
        private final String name;
        private final String type;
        private final double width;
        private final double height;
        private final Map<String, String> properties;

        private TemplateData(String name, String type, double width, double height, Map<String, String> properties) {
            this.name = name;
            this.type = type;
            this.width = width;
            this.height = height;
            this.properties = properties;
        }

        private static TemplateData empty() {
            return new TemplateData("", "", 0, 0, new HashMap<>());
        }
    }
}
