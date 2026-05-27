package dialogue.script;

import dialogue.model.DialoguePage;
import dialogue.model.DialogueScript;

import java.util.List;

public final class OpeningIntroFactory {
    private OpeningIntroFactory() {
    }

    public static DialogueScript create(String playerName) {
        return new DialogueScript(
                "opening_intro",
                List.of(
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Ngày xửa ngày xưa...",
                                        "Mai An Tiêm cùng người vợ của mình bị nhà vua đuổi ra một hòn đảo hoang xa đất liền.",
                                        "Nơi đây không có nhà cửa.",
                                        "Không có thức ăn.",
                                        "Chỉ có rừng sâu, thú dữ và những bí ẩn chưa ai biết tới.",
                                        "Để sống sót...",
                                        "Hai người phải tự thu thập gỗ, đá và thức ăn mỗi ngày."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Nhưng hòn đảo ấy không hề bình thường.",
                                        "Khi màn đêm buông xuống...",
                                        "Những sinh vật quỷ dị bắt đầu xuất hiện từ trong khu rừng sâu...",
                                        "Glory... Glory...",
                                        "Những âm thanh kỳ lạ vang vọng khắp hòn đảo."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Trong một đêm hỗn loạn...",
                                        "Người vợ của Mai An Tiêm đã bị yêu quái bắt đi.",
                                        "Bị bỏ lại một mình giữa hòn đảo xa lạ...",
                                        "Mai An Tiêm gần như tuyệt vọng.",
                                        "..."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Hỡi Mai An Tiêm...",
                                        "Ta là Thổ Địa cai quản hòn đảo này.",
                                        "Vợ ngươi vẫn còn sống.",
                                        "Nhưng đang bị giam giữ bởi Quỷ Đỏ.",
                                        "Nếu muốn cứu nàng...",
                                        "Ngươi phải sống sót và thu thập đủ 3 viên Ngọc Phong Ấn.",
                                        "Chỉ khi sống sót qua 3 đêm...",
                                        "Ngươi mới có cơ hội đối mặt với Quỷ Đỏ.",
                                        "Hãy đi theo ta...",
                                        "Ta sẽ chỉ cho ngươi cách giải cứu vợ mình."
                                )
                        )
                )
        );
    }
}
