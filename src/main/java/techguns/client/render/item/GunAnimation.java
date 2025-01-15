package techguns.client.render.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;

public class GunAnimation {
	static final float PI=(float) Math.PI;
	static ICurveType C_ZERO = x -> 0.0f;
	static ICurveType C_LINEAR = x -> x;
	static ICurveType C_FAST = x -> 1f-((1f-x)*(1f-x));
	static ICurveType C_SLOW = x -> x*x;
	static ICurveType C_SMOOTH = x -> {
        float d = MathHelper.sin(PI*x*0.5f);
        return d*d;
    };
	static ICurveType C_SINUS = x -> MathHelper.sin(PI*x*2.0f);

	public static final GunAnimation genericRecoil = new GunAnimation().addTranslate(0, 0, 1.0f).addRotate(1.0f, 1.0f, 0, 0).addSegment(0, 0.25f, C_FAST, 0f, 1f).addSegment(0.25f, 1f, C_SMOOTH, 1f, 0f);
	public static final GunAnimation swayRecoil = new GunAnimation().addTranslate(1.0f, 0.75f, 0.50f).addRotate(1.0f, 1.0f, -0.75f, 0.25f).addSegment(0, 1.0f,C_SINUS, 0.0f, 1.0f);
	
	public static final GunAnimation genericReload = new GunAnimation().addTranslate(-0.15f, -0.05f, 0.15f).addRotate(1.0f, -0.5f, 1.0f, -0.25f).addSegment(0, 0.2f, C_SMOOTH, 0f, 1f).addSegment(0.2f, 0.8f, C_LINEAR, 1.0f, 1.0f).addSegment(0.8f, 1.0f, C_SMOOTH, 1.0f, 0.0f);
	public static final GunAnimation breechReload = new GunAnimation().addTranslate(0, 1.0f, -1.0f).addRotate(1.0f, -1.0f, 0f, 0f).addSegment(0, 0.25f, C_SMOOTH, 0f, 1f).addSegment(0.25f, 0.5f, C_LINEAR, 1f, 1f).addSegment(0.5f, 0.85f, C_SMOOTH, 1.0f, -0.25f).addSegment(0.85f, 1f, C_SMOOTH, -0.25f, 0f);

	public static final GunAnimation scopeRecoil = new GunAnimation().addTranslate(0.25f, 1.0f, 0.75f).addRotate(1.0f, 1.0f, 0, 0).addSegment(0, 0.25f, C_FAST, 0f, 1f).addSegment(0.25f, 1f, C_SMOOTH, 1f, 0f);
	public static final GunAnimation scopeRecoilAdv = new GunAnimation().addTranslate(1.0f, 0f, 0f).addTranslate(0.0f, 1f, 0f).addTranslate(0.0f, 0f, 1f).addRotate(1.0f, 1.0f, 0, 0).addSegment(0, 0.25f, C_FAST, 0f, 1f).addSegment(0.25f, 1f, C_SMOOTH, 1f, 0f);
	
	public static final GunAnimation pulseRifleRecoil = new GunAnimation().addTranslate(0, 0, 1.0f).addRotate(1.0f, 1.0f, 0, 0).addSegment(0, 0.1f, C_FAST, 0f, 0.4f).addSegment(0.1f, 0.2f, C_SMOOTH, 0.4f, 0.3f).addSegment(0.2f, 0.40f, C_FAST, 0.3f, 1f).addSegment(0.40f, 1f, C_SMOOTH, 1f, 0f);
	
	public static final GunAnimation swordSweepRecoil = new GunAnimation().addRotate(1.0f, 0, 0, 1.0f).addSegment(0, 0.25f, C_FAST, 0f, 1f).addSegment(0.25f, 1f, C_SMOOTH, 1f, 0f);
	
	
	private final List<AnimationSegment> segments=new ArrayList<>();
	private final List<Transformation> transformations=new ArrayList<>();
	
	public GunAnimation() {
	}
	
	public GunAnimation addSegment(float start, float end, ICurveType curve, float val1, float val2) {
		this.segments.add(new AnimationSegment(start, end, curve, val1, val2));
		return this;
	}
	public GunAnimation addTranslate(float x, float y, float z) {
		this.transformations.add((float f,boolean mirror)-> GlStateManager.translate(mirror  ? (x*-f) : (x*f), y*f, z*f));
		return this;
	}
	public GunAnimation addRotate(float angle, float x, float y, float z) {
		this.transformations.add((float f,boolean mirror)-> GlStateManager.rotate(f*angle, x, mirror ? -y : y, mirror ? -z : z));
		return this;
	}
	
	static class AnimationSegment {
		
		public AnimationSegment(float start, float end, ICurveType curve, float val1, float val2) {
			super();
			this.start = start;
			this.end = end;
			this.curve = curve;
			this.val1 = val1;
			this.val2 = val2;
		}
		float start;
		float end;
		float val1;
		float val2;
		ICurveType curve;
		public float getValue(float progress) {
			float v = curve.f(progress);
			return val1 + v * (val2-val1);
		}
	}
	
	public interface ICurveType {
		float f(float x);
	}

	interface Transformation {
		void apply(float f, boolean mirror);
	}
	
	public void play(float progress, boolean mirror, float... magnitudes) {
		float prev = 0.0f;
		float value = 0.0f;
		for (AnimationSegment segment : segments) {
			if (progress > segment.start && progress <= segment.end) {
				float p = (progress-prev)/(segment.end-segment.start);
				value += segment.getValue(p);
			}
			prev = segment.end;
		}
		int i = 0;
		for (Transformation trans : transformations) {
			float v = value;
			if (magnitudes.length > i) v*=magnitudes[i++];
			trans.apply(v, mirror);
		}
	}
}
