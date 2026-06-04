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
                                        "Ng\u00e0y x\u1eeda ng\u00e0y x\u01b0a...",
                                        "Mai An Ti\u00eam c\u00f9ng ng\u01b0\u1eddi v\u1ee3 c\u1ee7a m\u00ecnh b\u1ecb nh\u00e0 vua \u0111u\u1ed5i ra m\u1ed9t h\u00f2n \u0111\u1ea3o hoang xa \u0111\u1ea5t li\u1ec1n.",
                                        "N\u01a1i \u0111\u00e2y kh\u00f4ng c\u00f3 nh\u00e0 c\u1eeda.",
                                        "Kh\u00f4ng c\u00f3 th\u1ee9c \u0103n.",
                                        "Ch\u1ec9 c\u00f3 r\u1eebng s\u00e2u, th\u00fa d\u1eef v\u00e0 nh\u1eefng b\u00ed \u1ea9n ch\u01b0a ai bi\u1ebft t\u1edbi.",
                                        "\u0110\u1ec3 s\u1ed1ng s\u00f3t...",
                                        "Hai ng\u01b0\u1eddi ph\u1ea3i t\u1ef1 thu th\u1eadp g\u1ed7, \u0111\u00e1 v\u00e0 th\u1ee9c \u0103n m\u1ed7i ng\u00e0y."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Nh\u01b0ng h\u00f2n \u0111\u1ea3o \u1ea5y kh\u00f4ng h\u1ec1 b\u00ecnh th\u01b0\u1eddng.",
                                        "Khi m\u00e0n \u0111\u00eam bu\u00f4ng xu\u1ed1ng...",
                                        "Nh\u1eefng sinh v\u1eadt qu\u1ef7 d\u1ecb b\u1eaft \u0111\u1ea7u xu\u1ea5t hi\u1ec7n t\u1eeb trong khu r\u1eebng s\u00e2u...",
                                        "Glory... Glory...",
                                        "Nh\u1eefng \u00e2m thanh k\u1ef3 l\u1ea1 vang v\u1ecdng kh\u1eafp h\u00f2n \u0111\u1ea3o."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "Trong m\u1ed9t \u0111\u00eam h\u1ed7n lo\u1ea1n...",
                                        "Ng\u01b0\u1eddi v\u1ee3 c\u1ee7a Mai An Ti\u00eam \u0111\u00e3 b\u1ecb y\u00eau qu\u00e1i b\u1eaft \u0111i.",
                                        "B\u1ecb b\u1ecf l\u1ea1i m\u1ed9t m\u00ecnh gi\u1eefa h\u00f2n \u0111\u1ea3o xa l\u1ea1...",
                                        "Mai An Ti\u00eam g\u1ea7n nh\u01b0 tuy\u1ec7t v\u1ecdng.",
                                        "..."
                                )
                        ),
                        new DialoguePage(
                                "",
                                "",
                                List.of(
                                        "H\u1ee1i t\u00ean ph\u00e0m nh\u00e2n kia...",
                                        "Ta l\u00e0 Th\u1ed5 \u0110\u1ecba cai qu\u1ea3n h\u00f2n \u0111\u1ea3o n\u00e0y.",
                                        "V\u1ee3 ng\u01b0\u01a1i v\u1eabn c\u00f2n s\u1ed1ng.",
                                        "Nh\u01b0ng \u0111ang b\u1ecb giam gi\u1eef b\u1edfi Qu\u1ef7 \u0110\u1ecf.",
                                        "N\u1ebfu mu\u1ed1n c\u1ee9u n\u00e0ng...",
                                        "Ng\u01b0\u01a1i ph\u1ea3i s\u1ed1ng s\u00f3t v\u00e0 thu th\u1eadp \u0111\u1ee7 2 vi\u00ean Ng\u1ecdc Phong \u1ea4n.",
                                        "Ch\u1ec9 khi s\u1ed1ng s\u00f3t qua 2 \u0111\u00eam...",
                                        "Ng\u01b0\u01a1i m\u1edbi c\u00f3 c\u01a1 h\u1ed9i \u0111\u1ed1i m\u1eb7t v\u1edbi Qu\u1ef7 \u0110\u1ecf.",
                                        "H\u00e3y \u0111i theo ta...",
                                        "Ta s\u1ebd ch\u1ec9 cho ng\u01b0\u01a1i c\u00e1ch gi\u1ea3i c\u1ee9u v\u1ee3 m\u00ecnh."
                                )
                        )
                )
        );
    }
}
