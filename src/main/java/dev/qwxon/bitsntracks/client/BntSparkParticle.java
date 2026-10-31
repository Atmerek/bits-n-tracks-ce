package dev.qwxon.bitsntracks.client;

import dev.qwxon.bitsntracks.index.BitsNTracksParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

public class BntSparkParticle extends TextureSheetParticle {
    private static final float HOT_GREEN = 0.95F;
    private static final float HOT_BLUE = 0.65F;
    private static final float COOL_GREEN = 0.35F;
    private static final float COOL_BLUE = 0.05F;
    private static final float FADE_FROM = 0.6F;

    protected BntSparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 1.0F;
        this.friction = 0.94F;
        this.lifetime = 5 + this.random.nextInt(9);
        this.quadSize = 0.03F + this.random.nextFloat() * 0.03F;
        this.setColor(1.0F, HOT_GREEN, HOT_BLUE);
    }

    public static void register(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(BitsNTracksParticles.SPARK.get(), Provider::new);
    }

    @Override
    public void tick() {
        super.tick();
        float age = Mth.clamp((float)this.age / this.lifetime, 0.0F, 1.0F);
        this.setColor(1.0F, Mth.lerp(age, HOT_GREEN, COOL_GREEN), Mth.lerp(age, HOT_BLUE, COOL_BLUE));
        this.setAlpha(age < FADE_FROM ? 1.0F : 1.0F - (age - FADE_FROM) / (1.0F - FADE_FROM));
        if (this.onGround) {
            this.xd *= 0.6;
            this.zd *= 0.6;
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(
            SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd
        ) {
            BntSparkParticle particle = new BntSparkParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
