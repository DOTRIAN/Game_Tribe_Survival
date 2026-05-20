package entity;

/**
 * BaseCamp:
 * - Nha chinh/trai trung tam cua world sinh ton.
 * - Dieu kien thua: BaseCamp bi pha huy.
 */
public class BaseCamp extends Entity {
    // Constructor:
    // - Input: toa do va kich thuoc nha chinh.
    // - Output: entity camp co hp lon de phong thu nhieu dem.
    public BaseCamp(double x, double y, double width, double height, int maxHp) {
        super(x, y, width, height, 0, maxHp);
    }
}
