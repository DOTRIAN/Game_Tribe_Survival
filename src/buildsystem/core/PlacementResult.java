package buildsystem.core;

/**
 * PlacementResult:
 * - Ket qua validation placement dung chung cho preview flow va place flow.
 * - Ngoai boolean hop le, class nay con giu ly do invalid de debug/UI co the hien thi sau nay.
 */
public class PlacementResult {
    private static final PlacementResult VALID = new PlacementResult(true, "OK");

    private final boolean valid;
    private final String reason;

    private PlacementResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason == null ? "" : reason;
    }

    public static PlacementResult valid() {
        return VALID;
    }

    public static PlacementResult invalid(String reason) {
        return new PlacementResult(false, reason);
    }

    public boolean isValid() {
        return valid;
    }

    public String getReason() {
        return reason;
    }
}
