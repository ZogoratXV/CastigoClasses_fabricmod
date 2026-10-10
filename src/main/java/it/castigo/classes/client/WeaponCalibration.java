package it.castigo.classes.client;
import com.google.gson.JsonObject;
/** Bounded optional packet values. Old servers keep the original animation. */
public final class WeaponCalibration {
    private final JsonObject values;
    public WeaponCalibration(JsonObject packet){values=packet!=null&&packet.has("calibration")&&packet.get("calibration").isJsonObject()?packet.getAsJsonObject("calibration").deepCopy():new JsonObject();}
    public double get(String key){double fallback=key.endsWith("Intensity")?1:0;try{double n=values.get(key).getAsDouble();double max=key.endsWith("Intensity")||key.endsWith("X")||key.endsWith("Y")||key.endsWith("Z")?2:180;return Double.isFinite(n)?Math.clamp(n,key.endsWith("Intensity")?0:-max,max):fallback;}catch(RuntimeException e){return fallback;}}
}
