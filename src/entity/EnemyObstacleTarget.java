package entity;

import buildsystem.object.BuildObject;
import system.resource.ResourceNode;

public record EnemyObstacleTarget(BuildObject buildObject, ResourceNode resourceNode) {
    public static EnemyObstacleTarget forBuild(BuildObject buildObject) {
        return buildObject == null ? null : new EnemyObstacleTarget(buildObject, null);
    }

    public static EnemyObstacleTarget forResource(ResourceNode resourceNode) {
        return resourceNode == null ? null : new EnemyObstacleTarget(null, resourceNode);
    }

    public boolean isBuildObject() {
        return buildObject != null;
    }

    public boolean isResourceNode() {
        return resourceNode != null;
    }

    public boolean isAlive() {
        return (buildObject != null && buildObject.isAlive()) || (resourceNode != null && resourceNode.isAlive());
    }

    public double getCenterX() {
        return buildObject != null ? buildObject.getCenterX() : resourceNode.getCenterX();
    }

    public double getCenterY() {
        return buildObject != null ? buildObject.getCenterY() : resourceNode.getCenterY();
    }

    public double getCollisionX() {
        return buildObject != null ? buildObject.getCollisionX() : resourceNode.getCollisionX();
    }

    public double getCollisionY() {
        return buildObject != null ? buildObject.getCollisionY() : resourceNode.getCollisionY();
    }

    public double getCollisionWidth() {
        return buildObject != null ? buildObject.getCollisionWidth() : resourceNode.getCollisionWidth();
    }

    public double getCollisionHeight() {
        return buildObject != null ? buildObject.getCollisionHeight() : resourceNode.getCollisionHeight();
    }

    public int getCurrentHp() {
        return buildObject != null ? buildObject.getHealth() : resourceNode.getCurrentHp();
    }

    public String getDebugLabel() {
        if (buildObject != null && buildObject.getType() != null) {
            return buildObject.getType().name();
        }
        if (resourceNode != null) {
            return resourceNode.getResourceType().name();
        }
        return "UNKNOWN";
    }
}
