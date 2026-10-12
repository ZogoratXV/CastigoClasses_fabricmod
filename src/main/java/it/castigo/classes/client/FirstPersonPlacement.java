package it.castigo.classes.client;

/** PAL body-space: positive Y lowers the rig, positive Z moves it away from the camera. */
record FirstPersonPlacement(double x,double y,double z,double pitch,double yaw,double roll) {
    static FirstPersonPlacement of(CombatChoreography.Profile profile,boolean left,WeaponCalibration c){
        boolean two=PlayerClips.twoHandedProfile(profile);
        double x=Math.clamp(c.get("firstX"),-.25,.25)*(left?-1:1);
        double y=-Math.clamp(c.get("firstY"),-.15,.15);
        double z=-Math.clamp(c.get("firstZ"),-.15,.15);
        if(two){x+=left?-.18:.18;y+=.22;z+=.30;}
        else if(profile==CombatChoreography.Profile.BOW){y+=.18;z+=.20;}
        return new FirstPersonPlacement(x,y,z,Math.clamp(c.get("firstPitch"),-20,20),Math.clamp(c.get("firstYaw"),-20,20)*(left?-1:1),Math.clamp(c.get("firstRoll"),-20,20)*(left?-1:1));
    }
}
