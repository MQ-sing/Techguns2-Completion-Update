package techguns.util;

import net.minecraft.client.resources.I18n;
import techguns.Techguns;

public class TextUtil {
	public static String trans(String key){
		return I18n.format(key);
	}
	/**
	 * Trans with prefixing MODID.
	 */
	public static String transTG(String key){
		return I18n.format(Techguns.MODID+"."+key);
	}
}
