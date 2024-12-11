package ages.world.blocks;

import ages.world.meta.*;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
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
import mindustry.world.blocks.production.*;
import mindustry.world.meta.*;

import static ages.AgesVar.*;
import static arc.Core.atlas;

public class Thorn extends Wall {
    public int thornHealth;
    public float thornDamage;
    public float thornSizeMultiplier = 1.1f;
    public float thornSize;
    public float repairCool = 60f;
    public int repairAmount;
    public float stickyness = 0.5f;
    public float chanceBreak = 0.5F;
    public Effect breakEffect;
    public ItemStack repairItem;

    public TextureRegion thornRegion;

    public Sound breakSound = Sounds.rockBreak;
    public Sound repairSound = Sounds.pulse;

    public Thorn(String name) {
        super(name);

        this.update = true;
        this.configurable = true;
        this.thornSize = Vars.tilesize * this.thornSizeMultiplier;
        this.thornRegion = atlas.find(localizedName + "-thorn");
    }

    @Override
    public void init(){
        super.init();

        this.repairAmount = repairItem.amount / requirements[0].amount * thornHealth;
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
                return ((ThornBuild)e).heat / repairCool;
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
        }

        public void repair(Unit unit){
            if (!unit.hasItem()) return;
            if (unit.item() != repairItem.item || unit.stack.amount < repairItem.amount) return;

            unit.stack.amount -= repairItem.amount;
            this.thorn += repairAmount; //Math.min(repairAmount, thornHealth - repairAmount);
            heat = repairCool;
            repairSound.play();
        }

        public boolean canRepair() {
            return thorn < thornHealth;
        }


        public boolean thornBroken() {
            return thorn <= 0.0F;
        }

        public void updateTile(){
            Units.nearbyEnemies(team, x - size * thornSize / 2, y - size * thornSize / 2, size * thornSize, size * thornSize, this::triggered);
            Log.info(heat);
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
        }

        public void buildConfiguration(Table table){
            table.button(Core.bundle.format("thorn.repair", canRepair() && heat <= 0f ? "green" : "red"), () -> {
                if (heat <= 0f) Units.nearby(team, x - size * thornSize * 1.5f, y - size * thornSize * 1.5f, size * thornSize * 3f, size * thornSize * 3f, this::repair);

                Draw.color(Color.red);
                Lines.rect(x - size * thornSize * 1.5f, y - size * thornSize * 1.5f, size * thornSize * 1.5f, size * thornSize * 1.5f);
            });

            Draw.reset();
        }

        public void write(Writes write){
            super.write(write);
            write.f(thorn);
        }

        public void read(Reads read, byte revision){
            super.read(read, revision);
            thorn = read.f();
        }
    }
}