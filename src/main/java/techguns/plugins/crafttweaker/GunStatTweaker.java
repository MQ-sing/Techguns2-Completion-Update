package techguns.plugins.crafttweaker;

import crafttweaker.api.data.IData;
import stanhebben.zenscript.annotations.*;
import stanhebben.zenscript.value.IAny;
import techguns.TGuns;
import techguns.items.guns.GenericGun;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@ZenClass("mods.techguns.Guns")
@IterableMap(key="string",value="mods.techguns.GunStat")
public class GunStatTweaker implements Map<String,GunStatTweaker.GunStat>{
	HashMap<String, GunStat> map=new HashMap<>();
	GunStatTweaker(){
		try {
			for(Field field : TGuns.class.getDeclaredFields()){
				if(Modifier.isStatic(field.getModifiers()) && GenericGun.class.isAssignableFrom(field.getType())){
                    map.put(field.getName(),new GunStat((GenericGun) field.get(null)));
            	}
			}
		} catch (IllegalAccessException e) {
		//TODO
		}
	}

	@ZenGetter("size")
	@ZenMethod
	@Override
	public int size() {
		return map.size();
	}

	@ZenMethod
	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@ZenOperator(OperatorType.CONTAINS)
	@ZenMethod
	@Override
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}
	@ZenMemberGetter
	@ZenOperator(OperatorType.INDEXGET)
	@ZenMethod
	@Override
	public GunStat get(Object key) {
		return map.get(key);
	}
	@ZenMemberGetter
	@ZenOperator(OperatorType.INDEXSET)
	@ZenMethod
	@Override
	public GunStat put(String key, GunStat value) {
		if(map.containsKey(key))throw new UnsupportedOperationException("Add new keys into a immutable map");
		return map.put(key,value);
	}

	@Override
	public GunStat remove(Object key) {
		throw new UnsupportedOperationException("Modify a immutable map");
	}

	@Override
	public void putAll(Map<? extends String, ? extends GunStat> m) {
		throw new UnsupportedOperationException("Modify a immutable map");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("Modify a immutable map");
	}
	@ZenGetter("keySet")
	@ZenMethod
	@Override
	public Set<String> keySet() {
		return map.keySet();
	}
	@ZenGetter("values")
	@ZenMethod
	@Override
	public Collection<GunStat> values() {
		return map.values();
	}
	@ZenGetter("entrySet")
	@ZenMethod
	@Override
	public Set<Entry<String, GunStat>> entrySet() {
		return map.entrySet();
	}

	@ZenClass("mods.techguns.GunStat")
	public static class GunStat {
		GenericGun gun;
		GunStat(GenericGun gun){this.gun = gun;}
		@ZenGetter("damage")
		@ZenMethod
		public float getDamage(){
			return gun.damage;
		}
		@ZenSetter("damage")
		@ZenMethod
		public void setDamage(float damage){
			gun.damage=damage;
		}
		@ZenGetter("damageMin")
		@ZenMethod
		public float getDamageMin(){
			return gun.damageMin;
		}
		@ZenSetter("damageMin")
		@ZenMethod
		public void setDamageMin(float damageMin){
			gun.damageMin=damageMin;
		}
		@ZenGetter("damageDropStart")
		@ZenMethod
		public float getDamageDropStart(){
			return gun.damageDropStart;
		}
		@ZenSetter("damageDropStart")
		@ZenMethod
		public void setDamageDropStart(float damageDropStart){
			gun.damageDropEnd=damageDropStart;
		}
		@ZenGetter("damageDropEnd")
		@ZenMethod
		public float getDamageDropEnd(){
			return gun.damageDropEnd;
		}
		@ZenSetter("damageDropEnd")
		@ZenMethod
		public void setDamageDropEnd(float damageDropEnd){
			gun.damageDropEnd=damageDropEnd;
		}

		@ZenGetter("livingTicks")
		@ZenMethod
		public int getLivingTicks(){
			return gun.ticksToLive;
		}
		@ZenSetter("livingTicks")
		@ZenMethod
		public void setLivingTicks(int livingTicks){
			gun.ticksToLive=livingTicks;
		}
		@ZenGetter("bulletSpeed")
		@ZenMethod
		public float getBulletSpeed(){return gun.speed;}
		@ZenSetter("bulletSpeed")
		@ZenMethod
		public void setBulletSpeed(float bulletSpeed){gun.speed=bulletSpeed;}

		@ZenGetter("spread")
		@ZenMethod
		public float getSpread(){return gun.spread;}
		@ZenSetter("spread")
		@ZenMethod
		public void setSpread(float spread){gun.spread=spread;}

		@ZenGetter("silenced")
		@ZenMethod
		public boolean getSilenced(){return gun.silenced;}
		@ZenSetter("silenced")
		@ZenMethod
		public void setSilenced(boolean silenced){gun.silenced=silenced;}
	}
	static GunStatTweaker tweaker;

	@ZenMethod
	public static GunStatTweaker stats(){
		if(tweaker==null){
			tweaker = new GunStatTweaker();
		}
		return tweaker;
	}
}
