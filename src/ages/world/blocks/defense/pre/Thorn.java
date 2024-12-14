package ages.world.blocks.defense.pre;

import ages.world.meta.*;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.meta.*;

import static arc.Core.atlas;
import static mindustry.Vars.tilesize;

public class Thorn extends Wall {
    public int thornHealth;
    public float thornDamage;
    public float thornSizeMultiplier = 1.1f;
    public float thornSize;
    public float repairCool = 60f;
    public int repairAmount;
    public int repairRange = 1;
    public float stickyness = 0.5f;
    public float chanceBreak = 0.5F;
    public Effect breakEffect;
    public ItemStack repairItem;

    public TextureRegion thornRegion;

    public Sound breakSound = Sounds.rockBreak;
    public Sound repairSound = Sounds.breaks;

    public Thorn(String name) {
        super(name);

        this.update = true;
        this.configurable = true;
        this.thornSize = Vars.tilesize * this.thornSizeMultiplier;
        this.thornRegion = atlas.find(localizedName + "-thorn");
    }

    @Override
    public void setBars(){
        super.setBars();

        this.addBar("thorn", (e) -> {
            return new Bar("bar.thorn", Pal.plastanium, () -> {
                return ((ThornBuild)e).thorn / thornHealth;
            });
        });

        this.addBar("repair", (e) -> {
            return new Bar("bar.thorn-repair", Pal.heal, () -> {
                return 1 - ((ThornBuild)e).heat / repairCool;
            });
        });
    }

    @Override
    public void setStats(){
        super.setStats();
        this.stats.add(Stat.health, Core.bundle.format("thorn.thornHealth", thornHealth));

        if (chanceBreak > 0.0f) {
            this.stats.add(Stat.lightningChance, Core.bundle.format("thorn.chanceBreak", chanceBreak));
        }

        this.stats.add(AStat.repairItem, StatValues.items(false, repairItem));
    }

    public void repairItem(ItemStack item) {
        this.repairItem = item;
    }

    public class ThornBuild extends WallBuild {
        public float thorn;
        public float heat;
        public boolean r = false;
        public Unit repairer;

        public ThornBuild(){
            super();
            this.thorn = Thorn.this.thornHealth;
            this.heat = repairCool;
        }

        public void triggered(Unit unit){
            if (unit.isFlying() || thornBroken()) return;
            unit.damageContinuous(thornDamage / 60f);
            //TODO Modern thorns give continuous damage, and paralysis instead of knockback (L annotation processors)
            unit.vel.scl(stickyness);
            if (unit.hasWeapons()) unit.reloadMultiplier /= stickyness;
        }

        public void repair(Unit unit){
            r = true;
            repairer = unit;
            if (!unit.hasItem()) return;
            if (unit.item() != repairItem.item || unit.stack.amount < repairItem.amount) return;

            unit.stack.amount -= repairItem.amount;
            thorn += Math.min(repairAmount, thornHealth - thorn);
            heat = repairCool;
            repairSound.play();
        }

        public void drawRepair(Unit u){
            float xOffset = Mathf.random(-0.5f, 0.5f);
            float yOffset = Mathf.random(-0.5f, 0.5f);
            float dst = Mathf.dst(this.x, this.y, u.x + xOffset, u.y + yOffset);

            Draw.rect(repairItem.item.fullIcon, Mathf.approach(this.x, u.x + xOffset, 0.01f), Mathf.approach(this.y, u.y + yOffset, 0.01f), Mathf.approachDelta(5.0F, 0F, 1 / 60f), Mathf.approachDelta(5.0F, 0F, 1 / 60f));
            r = false;
        }

        public boolean canRepair() {
            return thorn < thornHealth;
        }


        public boolean thornBroken() {
            return thorn <= 0.0F;
        }

        public void updateTile(){
            Units.nearbyEnemies(team, x - size * thornSize / 2, y - size * thornSize / 2, size * thornSize, size * thornSize, this::triggered);
            if (canRepair() && heat > 0f) heat -= 1f;
        }

        @Override
        public void damage(float damage){
            super.damage(damage);

            if (Math.random() <= chanceBreak && !thornBroken()) {
                float thornDmg = thornBroken() ? 0.0f : Math.min(damage, thorn);
                thorn -= thornDmg;
                if (thorn <= damage){
                    breakSound.play();
                    //breakEffect.at(this);

                    //breakDraw();
                }
            }
        }

        @Override
        public void draw(){
            super.draw();

            //TODO Create thorn break effect
            if (r){
                Unit u = repairer;
                float xOffset = Mathf.random(-0.5f, 0.5f);
                float yOffset = Mathf.random(-0.5f, 0.5f);
                float dst = Mathf.dst(this.x, this.y, u.x + xOffset, u.y + yOffset);

                Tmp.v1.trns(Angles.angle(u.x, u.y, this.x, this.y), 4.0F);

                float ix = u.x + Tmp.v1.x;
                float iy = u.y + Tmp.v1.y;

                Draw.rect(repairItem.item.fullIcon, ix, iy, 5.0F, 5.0F);
                if (ix == u.x && iy == u.y) r = false;
            }
        }

        public void buildConfiguration(Table table){
            table.button(Core.bundle.format("thorn.repair"), () -> {
                if (heat <= 0f) Units.nearby(team, x, y, size * thornSize / 2 + tilesize * repairRange, this::repair);
            });
        }

        @Override
        public void drawConfigure(){
            Draw.alpha(Mathf.absin(0.5f, 0.5f));
            Drawf.dashCircle(x, y, size * thornSize / 2 + tilesize * repairRange, Tmp.c1.set(Color.red).a(Mathf.absin(4.0f, 1.0f)));
            super.drawConfigure();
        }

        public void write(Writes write){
            super.write(write);
            write.f(thorn);
            write.f(heat);
        }

        public void read(Reads read, byte revision){
            super.read(read, revision);
            thorn = read.f();
            heat = read.f();
        }
    }
}