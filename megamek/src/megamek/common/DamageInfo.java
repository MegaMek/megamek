package megamek.common;

import megamek.common.weapons.DamageType;

public record DamageInfo(
        Entity te,
        HitData hit,
        int damage,
        boolean ammoExplosion,
        DamageType damageType,
        boolean damageIS,
        boolean areaSatArty,
        boolean throughFront,
        boolean underWater,
        boolean nukeS2S
    ) {

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage
    ){
        this(
              te,
              hit,
              damage,
              false,
              DamageType.NONE,
              false,
              false
        );
    }

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage,
          boolean ammoExplosion
    ){
        this(
              te,
              hit,
              damage,
              ammoExplosion,
              DamageType.NONE,
              false,
              false
        );
    }

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS
    ){
        this(
              te,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              false
        );
    }

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty
    ){
        this(
              te,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              true
        );
    }

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty,
          boolean throughFront
    ){
        this(
              te,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              throughFront,
              false,
              false
        );
    }

    public DamageInfo(
          Entity te,
          HitData hit,
          int damage,
          boolean ammoExplosion,
          DamageType bFrag,
          boolean damageIS,
          boolean areaSatArty,
          boolean throughFront,
          boolean underWater
    ) {
        this(
              te,
              hit,
              damage,
              ammoExplosion,
              bFrag,
              damageIS,
              areaSatArty,
              throughFront,
              underWater,
              false
        );
    }
}
