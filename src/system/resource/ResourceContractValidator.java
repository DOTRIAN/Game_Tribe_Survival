package system.resource;

import map.MapObjectData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ResourceContractValidator:
 * - Validate nhanh object map theo "contract" da thong nhat.
 * - Muc tieu: map loi thi biet ngay object nao sai key/sai value.
 * - Class nay CHI validate du lieu, khong tao gameplay object.
 */
public class ResourceContractValidator {

    /**
     * validate:
     * - Duyet danh sach object dau vao va tra ve danh sach loi dang String.
     * - Neu list rong => du lieu dat tieu chuan toi thieu.
     */
    public List<String> validate(List<MapObjectData> objects) {
        List<String> errors = new ArrayList<>();
        if (objects == null) {
            errors.add("Object list is null.");
            return errors;
        }

        for (MapObjectData object : objects) {
            if (object == null) {
                errors.add("Found null object entry.");
                continue;
            }

            Map<String, String> props = object.getProperties();
            if (props == null || props.isEmpty()) {
                continue;
            }

            boolean hasKind = hasValue(props, "kind");
            boolean hasDrop = hasValue(props, "dropItem");
            boolean hasMaxHp = hasValue(props, "maxHp");

            // Chi coi object la "resource candidate" khi co it nhat 1 key lien quan.
            boolean maybeResource = hasKind || hasDrop || hasMaxHp;
            if (!maybeResource) {
                continue;
            }

            int objectId = object.getId();
            if (!hasKind) {
                errors.add("Object id=" + objectId + " missing required property: kind");
            }
            if (!hasDrop) {
                errors.add("Object id=" + objectId + " missing required property: dropItem");
            }
            if (!hasMaxHp) {
                errors.add("Object id=" + objectId + " missing required property: maxHp");
            }

            if (hasMaxHp && parseInt(props.get("maxHp"), -1) <= 0) {
                errors.add("Object id=" + objectId + " has invalid maxHp (must be > 0)");
            }

            int min = parseInt(props.get("dropMin"), 1);
            int max = parseInt(props.get("dropMax"), min);
            if (min < 0 || max < 0 || min > max) {
                errors.add("Object id=" + objectId + " invalid drop range: dropMin=" + min + ", dropMax=" + max);
            }
        }

        return errors;
    }

    private boolean hasValue(Map<String, String> props, String key) {
        String value = props.get(key);
        return value != null && !value.trim().isEmpty();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}

