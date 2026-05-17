package entity;

/**
 * TreeEntity:
 * - Lop cay/toa do static don gian de sau nay co the dua vao damage/collision system chung.
 * - Hien tai game dang sinh resource tree tu map tile/object thay vi spawn TreeEntity truc tiep,
 *   nhung class nay duoc tao san de cau truc entity dong bo theo yeu cau.
 */
public class TreeEntity extends Entity {
    public TreeEntity(double x, double y, double width, double height, int maxHp) {
        super(x, y, width, height, 0, maxHp);
    }
}
