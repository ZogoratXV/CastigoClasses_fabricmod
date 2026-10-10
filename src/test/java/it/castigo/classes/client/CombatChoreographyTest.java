package it.castigo.classes.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CombatChoreographyTest {
    @Test void everyProfileReturnsToRestAndPreparationStartsNeutral(){for(var p:CombatChoreography.Profile.values()){assertEquals(CombatChoreography.REST,CombatChoreography.sample(p,false,1,0));var f=CombatChoreography.sample(p,true,0,0);assertEquals(0,f.main().pitch());assertEquals(0,f.z());assertEquals(CombatChoreography.REST,CombatChoreography.sample(p,false,Double.NaN,0));}}
    @Test void swordAlternatesAndContainsTorsoMotion(){var a=CombatChoreography.sample(CombatChoreography.Profile.SWORD,false,.2,0);var b=CombatChoreography.sample(CombatChoreography.Profile.SWORD,false,.2,1);assertEquals(-a.main().yaw(),b.main().yaw());assertEquals(-a.body().yaw(),b.body().yaw());assertNotEquals(0,a.body().yaw());}
    @Test void bowStaysLowAndOutsideCenter(){for(int i=0;i<100;i++){var f=CombatChoreography.sample(CombatChoreography.Profile.BOW,false,i/100d,0);assertTrue(f.y()<=0);assertTrue(f.x()>=0);assertTrue(Math.abs(f.pitch())<10);assertTrue(Math.abs(f.yaw())<10);}}
    @Test void posesAreFiniteAndContinuousAtKeyframes(){for(var p:CombatChoreography.Profile.values())for(double t:new double[]{.22,.55,1}){var a=CombatChoreography.sample(p,false,t-1e-7,0);var b=CombatChoreography.sample(p,false,t,0);assertEquals(a.main().pitch(),b.main().pitch(),.001);assertEquals(a.z(),b.z(),.001);}}
    @Test void unknownProfilesFallBackWithoutAnimating(){assertEquals(CombatChoreography.Profile.VANILLA,CombatChoreography.parse("UNKNOWN"));assertEquals(CombatChoreography.REST,CombatChoreography.sample(CombatChoreography.parse("UNKNOWN"),false,.2,0));}
}
