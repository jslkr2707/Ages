package ages.world.blocks.turrets.pre;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SettingsMenuDialog.SettingsTable.*;
import mindustry.world.blocks.defense.turrets.*;

import static arc.Core.atlas;

public class Catapult extends ItemTurret {
    public float reloadCapacity;
    public float overloadChance = 0.1f;
    public float overloadCool = 60f;
    public final int timerOverload;
    public boolean doOverload = true;
    public Sound ammoSound = Sounds.none;
    
    public Catapult(String name){
        super(name);

        this.configurable = true;
        this.timerOverload = this.timers++;
    }



    @Override
    public void setBars(){
        super.setBars();
        if (doOverload) this.addBar("heat", (entity) -> {
            return new Bar(Core.bundle.format("bar.overload"), Pal.spore, () -> {
                return ((CatapultBuild)entity).heatO / overloadCool;
            });
        });
    }

    public class CatapultBuild extends ItemTurret.ItemTurretBuild {
        public int ammos = 1;
        public boolean overload = false;
        public float heatO = 0f;

        public CatapultBuild(){
            super();
        }

        public boolean overloaded(){
            return doOverload && overload;
        }

        @Override
        public void updateTile(){
            super.updateTile();

            if (overloaded()) {
                this.reloadCounter = 0f;
                this.heatO += 1f;
                if (this.timer(timerOverload, overloadCool)) {
                    overload = false;
                    heatO = 0f;
                }
            }
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(Styles.grayPanel, pane -> {
                pane.label(() -> Core.bundle.format("catapult.reloads", this.ammos)).marginBottom(8f);
                pane.row();
                pane.table(Styles.black, b -> {
                    b.button(atlas.drawable("icon.minus"), () -> {
                        if (ammos > 1) {
                            ammos -= 1;
                            ammoSound.play();
                        }
                    });

                    b.button(Icon.add, () -> {
                        if (ammos < reloadCapacity) {
                            ammos += 1;
                            ammoSound.play();
                        }
                    } );
                }).center().marginTop(8f);
            }).center().growX().margin(15f);
        }

        @Override
        protected void shoot(BulletType type){
            Catapult.this.shoot.shots = ammos;
            Catapult.this.ammoPerShot = ammos;
            Catapult.this.shoot.firstShotDelay = ammos * 30f;
            super.shoot(type);
            if (Mathf.chance(overloadChance / reloadCapacity * ammos)) {
                overload = true;
                this.timer.reset(timerOverload, 0f);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            ammos = read.i();
            super.read(read, revision);
        }

        @Override
        public void write(Writes write) {
            write.f(ammos);
            super.write(write);
        }
    }


}
