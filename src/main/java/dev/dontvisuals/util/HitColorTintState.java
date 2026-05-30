package dev.dontvisuals.util;

/**
 * Хранит флаг "текущая рендерящаяся entity получила урон".
 * Устанавливается прямо перед render() каждой entity через LivingEntityRendererMixin.
 */
public final class HitColorTintState {
    private HitColorTintState() {}

    public static final ThreadLocal<Boolean> SHOULD_TINT = ThreadLocal.withInitial(() -> false);
}
