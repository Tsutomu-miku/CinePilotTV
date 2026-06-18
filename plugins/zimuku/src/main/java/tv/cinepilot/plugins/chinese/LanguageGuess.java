package tv.cinepilot.plugins.chinese;

import java.util.Locale;

/**
 * Guesses a subtitle's human-readable language tag from its filename /
 * description string. Handles the Chinese shorthand conventions that
 * appear ubiquitously on zimuku / shooter / a4k:
 * <ul>
 *   <li>{@code .chs}, {@code .gb}, {@code简体} → 简体中文</li>
 *   <li>{@code .cht}, {@code .big5}, {@code繁体} → 繁体中文</li>
 *   <li>{@code .eng}/{@code .en}, {@code英语/英文} → 英文</li>
 *   <li>{@code简英}/{@code繁英}/… → 双语</li>
 *   <li>{@code .jpn}/{@code日语} → 日文</li>
 *   <li>{@code .kor}/{@code韩语} → 韩文</li>
 * </ul>
 *
 * <p>Output strings are chosen to match what the ChineseSubFinder project
 * uses internally (so language-matching logic in the wider CinePilot
 * ecosystem can reuse the same vocabulary if needed).
 */
final class LanguageGuess {

    static String from(String... inputs) {
        StringBuilder joined = new StringBuilder();
        for (String s : inputs) {
            if (s != null) joined.append(' ').append(s.toLowerCase(Locale.ROOT));
        }
        String lower = joined.toString();
        // Presence of each language component — determined by tokens.
        boolean hasSimple  = lower.contains("chs")  || lower.contains("简体")
                || lower.contains("gb")   || lower.contains("简中");
        boolean hasTrad    = lower.contains("cht")  || lower.contains("繁体")
                || lower.contains("big5") || lower.contains("繁中");
        boolean hasTokenJian = lower.contains("简");
        boolean hasTokenFan  = lower.contains("繁");
        boolean hasEnglish   = lower.contains("eng") || lower.contains("english")
                || lower.contains("英文") || lower.contains("英语")
                || lower.contains(".en.") || lower.endsWith(".en");
        boolean hasJapanese = lower.contains("jpn") || lower.contains("日语")
                || lower.contains("日文") || lower.contains("日本語");
        boolean hasKorean   = lower.contains("kor") || lower.contains("韩语")
                || lower.contains("한국어");
        // Explicit bilingual / 双语 token — implies both Chinese and English.
        boolean bilingualToken = lower.contains("双语") || lower.contains("简英")
                || lower.contains("繁英") || lower.contains("中英")
                || lower.contains("chinese&english") || lower.contains("chs&eng")
                || lower.contains("cht&eng");
        if (bilingualToken) hasEnglish = true;

        // A "简繁英" / "中英特效" etc. token means all three are present.
        boolean allThree = lower.contains("简繁英") || lower.contains("简繁中英")
                || (hasTokenJian && hasTokenFan && (hasEnglish || bilingualToken));

        // Determine which Chinese variants are present (taking "简英" to mean
        // simplified + English, "繁英" to mean traditional + English).
        boolean hasSimpleChinese = hasSimple
                || (bilingualToken && !hasTrad && !hasTokenFan)  // "简英" / "中英" / "双语" default to simple
                || (hasTokenJian && !hasTokenFan);
        boolean hasTraditionalChinese = hasTrad
                || (hasTokenFan && !hasTokenJian);

        if (allThree) return "简繁英双语";
        if (hasSimpleChinese && hasTraditionalChinese && hasEnglish) return "简繁英双语";
        if (hasTraditionalChinese && hasEnglish) return "繁英双语";
        if (hasSimpleChinese && hasEnglish) return "简英双语";
        if (hasSimpleChinese && hasTraditionalChinese) return "简繁双语";
        if (hasSimple) return "简体中文";
        if (hasTrad) return "繁体中文";
        if (hasTokenJian && !hasTokenFan) return "简体中文";
        if (hasTokenFan && !hasTokenJian) return "繁体中文";
        if (!hasSimple && !hasTrad && !hasTokenJian && !hasTokenFan
                && (lower.contains("中文") || lower.contains("zh") || lower.contains("chn"))) {
            return "简体中文";
        }
        if (hasEnglish) return "英文";
        if (hasJapanese) return "日文";
        if (hasKorean) return "韩文";
        return "";
    }

    /**
     * True if the language tag is (or contains) Chinese in any form
     * (simplified, traditional, bilingual).
     */
    static boolean isChinese(String language) {
        if (language == null) return false;
        return language.contains("简")
                || language.contains("繁")
                || language.contains("中文")
                || language.contains("zh")
                || language.contains("Chn")
                || language.contains("chn");
    }

    /** Priority weight for ranking: higher = more preferred for Chinese users. */
    static int weight(String language) {
        if (language == null) return 0;
        if (language.contains("简英") || language.contains("简繁英")) return 5;
        if (language.contains("繁英")) return 4;
        if (language.contains("简繁")) return 3;
        if (language.contains("简体") || language.contains("简中")) return 2;
        if (language.contains("繁体") || language.contains("繁中")) return 1;
        if (isChinese(language)) return 1;
        if (language.contains("英")) return 0;
        return -1;
    }

    private LanguageGuess() {}
}
