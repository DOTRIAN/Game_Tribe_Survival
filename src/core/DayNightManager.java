package core;

public class DayNightManager {
    public enum Phase {
        DAY,
        WARNING,
        NIGHT_WAVE_1,
        NIGHT_WAVE_2,
        DAWN
    }

    private static final long SECOND_NS = 1_000_000_000L;
    private static final long DAY_DURATION_NS = 300L * SECOND_NS;
    private static final long WARNING_DURATION_NS = 60L * SECOND_NS;
    private static final long WAVE_1_DURATION_NS = 90L * SECOND_NS;
    private static final long WAVE_2_DURATION_NS = 90L * SECOND_NS;
    private static final long DAWN_DURATION_NS = 30L * SECOND_NS;
    private static final long CYCLE_DURATION_NS = DAY_DURATION_NS
            + WARNING_DURATION_NS
            + WAVE_1_DURATION_NS
            + WAVE_2_DURATION_NS
            + DAWN_DURATION_NS;
    private long cycleStartedAtNs = -1L;

    public void reset(long nowNs) {
        cycleStartedAtNs = nowNs;
    }

    public Phase getPhase(long nowNs) {
        long t = getTimeInCycle(nowNs);
        if (t < DAY_DURATION_NS) {
            return Phase.DAY;
        }
        if (t < DAY_DURATION_NS + WARNING_DURATION_NS) {
            return Phase.WARNING;
        }
        if (t < DAY_DURATION_NS + WARNING_DURATION_NS + WAVE_1_DURATION_NS) {
            return Phase.NIGHT_WAVE_1;
        }
        if (t < DAY_DURATION_NS + WARNING_DURATION_NS + WAVE_1_DURATION_NS + WAVE_2_DURATION_NS) {
            return Phase.NIGHT_WAVE_2;
        }
        return Phase.DAWN;
    }

    public int getDay(long nowNs) {
        ensureStarted(nowNs);
        long elapsed = Math.max(0L, nowNs - cycleStartedAtNs);
        return 1 + (int) (elapsed / CYCLE_DURATION_NS);
    }

    public int getHour(long nowNs) {
        int totalMinutes = getGameMinuteOfDay(nowNs);
        return totalMinutes / 60;
    }

    public int getMinute(long nowNs) {
        int totalMinutes = getGameMinuteOfDay(nowNs);
        return totalMinutes % 60;
    }

    public String getClockText(long nowNs) {
        return String.format("%02d:%02d", getHour(nowNs), getMinute(nowNs));
    }

    public String getPeriodLabel(long nowNs) {
        Phase phase = getPhase(nowNs);
        return (phase == Phase.NIGHT_WAVE_1 || phase == Phase.NIGHT_WAVE_2 || phase == Phase.DAWN)
                ? "Night"
                : "Day";
    }

    public String getTimeTitle(long nowNs) {
        return getPeriodLabel(nowNs) + " " + getDay(nowNs);
    }

    public String getCountdownText(long nowNs) {
        long remainingNs = getRemainingPhaseDurationNs(nowNs);
        long totalSeconds = Math.max(0L, (remainingNs + SECOND_NS - 1L) / SECOND_NS);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d left", minutes, seconds);
    }

    public String getTimeIcon(long nowNs) {
        return isNight(nowNs) ? "\uD83C\uDF19" : "\u2600";
    }

    public boolean isNight(long nowNs) {
        Phase phase = getPhase(nowNs);
        return phase == Phase.NIGHT_WAVE_1 || phase == Phase.NIGHT_WAVE_2 || phase == Phase.DAWN;
    }

    public double getDarknessAlpha(long nowNs) {
        int minuteOfDay = getGameMinuteOfDay(nowNs);
        if (minuteOfDay < 360 || minuteOfDay >= 1080) {
            if (minuteOfDay >= 1080 && minuteOfDay < 1140) {
                return lerpByMinute(minuteOfDay, 1080, 1140, 0.25, 0.55);
            }
            if (minuteOfDay >= 1140 && minuteOfDay < 1380) {
                return lerpByMinute(minuteOfDay, 1140, 1380, 0.55, 0.70);
            }
            if (minuteOfDay >= 1380) {
                return lerpByMinute(minuteOfDay, 1380, 1440, 0.70, 0.78);
            }
            return lerpByMinute(minuteOfDay, 180, 360, 0.78, 0.20);
        }
        if (minuteOfDay >= 180 && minuteOfDay < 360) {
            return lerpByMinute(minuteOfDay, 180, 360, 0.78, 0.20);
        }
        if (minuteOfDay >= 360 && minuteOfDay < 1080) {
            return 0.0;
        }
        return 0.0;
    }

    public String getPhaseName(long nowNs) {
        return getPhase(nowNs).name();
    }

    public String getScheduleDebugText(long nowNs) {
        String label = switch (getPhase(nowNs)) {
            case DAY -> "Day";
            case WARNING -> "Warning";
            case NIGHT_WAVE_1, NIGHT_WAVE_2 -> "Night";
            case DAWN -> "Dawn";
        };
        return label + ": " + getCountdownText(nowNs);
    }

    public String getAnnouncement(long nowNs) {
        Phase phase = getPhase(nowNs);
        if (phase == Phase.WARNING) {
            return "Màn đêm sắp xuống.\nHãy dựng rào và chuẩn bị vũ khí.";
        }
        if (phase == Phase.DAWN) {
            return "Trời sắp sáng.\nQuái đang rút khỏi trại.";
        }
        return null;
    }

    private long getRemainingPhaseDurationNs(long nowNs) {
        long t = getTimeInCycle(nowNs);
        if (t < DAY_DURATION_NS) {
            return DAY_DURATION_NS - t;
        }
        long warningEnd = DAY_DURATION_NS + WARNING_DURATION_NS;
        if (t < warningEnd) {
            return warningEnd - t;
        }
        long wave1End = warningEnd + WAVE_1_DURATION_NS;
        if (t < wave1End) {
            return wave1End - t;
        }
        long wave2End = wave1End + WAVE_2_DURATION_NS;
        if (t < wave2End) {
            return wave2End - t;
        }
        long dawnEnd = wave2End + DAWN_DURATION_NS;
        return Math.max(0L, dawnEnd - t);
    }

    private int getGameMinuteOfDay(long nowNs) {
        long t = getTimeInCycle(nowNs);
        if (t < DAY_DURATION_NS) {
            double progress = (double) t / DAY_DURATION_NS;
            return clampMinute((int) Math.floor(360 + progress * 720.0));
        }

        long warningStart = DAY_DURATION_NS;
        if (t < warningStart + WARNING_DURATION_NS) {
            double progress = (double) (t - warningStart) / WARNING_DURATION_NS;
            return clampMinute((int) Math.floor(1080 + progress * 60.0));
        }

        long wave1Start = warningStart + WARNING_DURATION_NS;
        if (t < wave1Start + WAVE_1_DURATION_NS) {
            double progress = (double) (t - wave1Start) / WAVE_1_DURATION_NS;
            return clampMinute((int) Math.floor(1140 + progress * 240.0));
        }

        long wave2Start = wave1Start + WAVE_1_DURATION_NS;
        if (t < wave2Start + WAVE_2_DURATION_NS) {
            double progress = (double) (t - wave2Start) / WAVE_2_DURATION_NS;
            return clampMinute((int) Math.floor(1380 + progress * 240.0));
        }

        long dawnStart = wave2Start + WAVE_2_DURATION_NS;
        double progress = (double) (t - dawnStart) / DAWN_DURATION_NS;
        return clampMinute((int) Math.floor(180 + progress * 180.0));
    }

    private int clampMinute(int minuteOfDay) {
        int wrapped = minuteOfDay % 1440;
        return wrapped < 0 ? wrapped + 1440 : wrapped;
    }

    private double lerpByMinute(int minuteOfDay, int startMinute, int endMinute, double startAlpha, double endAlpha) {
        if (endMinute <= startMinute) {
            return endAlpha;
        }
        double progress = (double) (minuteOfDay - startMinute) / (endMinute - startMinute);
        progress = Math.max(0.0, Math.min(1.0, progress));
        return startAlpha + (endAlpha - startAlpha) * progress;
    }

    private long getTimeInCycle(long nowNs) {
        ensureStarted(nowNs);
        long elapsed = Math.max(0L, nowNs - cycleStartedAtNs);
        return elapsed % CYCLE_DURATION_NS;
    }

    private void ensureStarted(long nowNs) {
        if (cycleStartedAtNs < 0L) {
            cycleStartedAtNs = nowNs;
        }
    }

    private String formatDuration(long durationNs) {
        long totalSeconds = Math.max(0L, durationNs / SECOND_NS);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes > 0 && seconds > 0) {
            return minutes + "m" + seconds + "s";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
    }
}
