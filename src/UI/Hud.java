package ui;

import entity.Player;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;



public class Hud {
    // ===== HUD Layout Config =====
    // Toa do goc trai tren cua khung HUD.
    private static final double PANEL_X = 16;
    private static final double PANEL_Y = 16;
    // Thu nho panel so voi ban truoc de tong the gon hon.
    private static final double PANEL_WIDTH = 210;
    private static final double PANEL_HEIGHT = 48;

    // Vi tri va kich thuoc thanh HP nam ben trong panel.
    // Gia tri nay duoc tinh theo PANEL_* de de canh chinh dong bo.
    private static final double HP_BAR_X = PANEL_X + 12;
    private static final double HP_BAR_Y = PANEL_Y + 18;
    // Thanh HP nho hon theo yeu cau.
    private static final double HP_BAR_WIDTH = 170;
    private static final double HP_BAR_HEIGHT = 12;

    // Vi tri Y cho text HP; X se duoc tinh dong de canh giua vao thanh HP.
    private static final double HP_TEXT_Y = HP_BAR_Y + 10;

    // Bo goc cho panel va hp bar, de nhin mem hon.
    private static final double PANEL_ARC = 10;
    private static final double BAR_ARC = 6;

    // Cho phep debug toa do neu can trong tuong lai.
    // Dang dat false nen se KHONG hien thi Player X/Y.
    private static final boolean SHOW_COORDINATES = false;

    public void render(GraphicsContext graphicsContext, Player player) {
        // ===== 1) Ve nen panel HUD =====
        // Mau den trong suot nhe de tach HUD khoi background map.
        graphicsContext.setFill(Color.color(0, 0, 0, 0.35));
        graphicsContext.fillRoundRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_ARC, PANEL_ARC);

        // Ve vien sang nhe de panel ro net hon.
        graphicsContext.setStroke(Color.color(1, 1, 1, 0.3));
        graphicsContext.strokeRoundRect(PANEL_X, PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_ARC, PANEL_ARC);

        // ===== 2) Ve thanh HP background =====
        // Lop nen toi phia sau de thay phan mau con lai / da mat.
        graphicsContext.setFill(Color.web("#2a2a2a"));
        graphicsContext.fillRoundRect(HP_BAR_X, HP_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT, BAR_ARC, BAR_ARC);

        // ===== 3) Tinh ti le HP =====
        // hpRatio nam trong [0..1]:
        // 0 = het mau, 1 = day mau.
        double hpRatio = 0;
        if (player.getMaxHp() > 0) {
            hpRatio = (double) player.getHp() / player.getMaxHp();
        }
        // Clamp de tranh loi ve (am hoac >1) neu du lieu bat thuong.
        hpRatio = Math.max(0, Math.min(1, hpRatio));

        // ===== 4) Ve phan mau hien tai =====
        // Chieu rong phan mau = tong chieu rong * hpRatio.
        double filledWidth = HP_BAR_WIDTH * hpRatio;
        graphicsContext.setFill(Color.web("#cf3f3f"));
        graphicsContext.fillRoundRect(HP_BAR_X, HP_BAR_Y, filledWidth, HP_BAR_HEIGHT, BAR_ARC, BAR_ARC);

        // Ve vien cho thanh HP de tach layer dep hon.
        graphicsContext.setStroke(Color.color(0, 0, 0, 0.55));
        graphicsContext.strokeRoundRect(HP_BAR_X, HP_BAR_Y, HP_BAR_WIDTH, HP_BAR_HEIGHT, BAR_ARC, BAR_ARC);

        // ===== 5) Ve text HP =====
        // Dat text o GIUA thanh HP de:
        // - khong bi tran panel khi chuoi dai (vd: 100/100)
        // - giao dien can doi hon so voi dat ben phai
        graphicsContext.setFill(Color.web("#f3f3f3"));
        String hpText = player.getHp() + "/" + player.getMaxHp();
        double hpTextWidth = measureTextWidth(graphicsContext, hpText);
        double hpTextX = HP_BAR_X + (HP_BAR_WIDTH - hpTextWidth) / 2;
        graphicsContext.fillText(hpText, hpTextX, HP_TEXT_Y);

        // ===== 6) Debug coordinates (dang tat) =====
        // Giu lai block nay de sau nay can debug player position thi bat lai nhanh.
        if (SHOW_COORDINATES) {
            graphicsContext.setFill(Color.BLACK);
            graphicsContext.fillText("Player X: " + player.getX(), 20, 110);
            graphicsContext.fillText("Player Y: " + player.getY(), 20, 135);
        }
    }

    // Do rong text theo font hien tai cua GraphicsContext.
    // JavaFX Canvas khong co ham do text truc tiep, nen dung Text helper.
    private double measureTextWidth(GraphicsContext graphicsContext, String text) {
        Text helper = new Text(text);
        helper.setFont(graphicsContext.getFont());
        return helper.getLayoutBounds().getWidth();
    }


}
