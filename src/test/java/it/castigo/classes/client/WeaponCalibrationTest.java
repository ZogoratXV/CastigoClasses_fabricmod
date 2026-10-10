package it.castigo.classes.client;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WeaponCalibrationTest {
    @Test void absentCalibrationKeepsLegacyPose(){var c=new WeaponCalibration(null);assertEquals(0,c.get("firstY"));assertEquals(1,c.get("firstIntensity"));assertEquals(1,c.get("thirdIntensity"));}
    @Test void untrustedValuesAreBoundedAndMalformedFallBack(){var c=new WeaponCalibration(JsonParser.parseString("{\"calibration\":{\"firstX\":99,\"thirdIntensity\":-1,\"firstYaw\":999,\"firstY\":\"bad\"}}").getAsJsonObject());assertEquals(2,c.get("firstX"));assertEquals(0,c.get("thirdIntensity"));assertEquals(180,c.get("firstYaw"));assertEquals(0,c.get("firstY"));}
}
