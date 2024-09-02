package mindustry.graphics;

import arc.*;
import arc.func.Boolf;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.ai.types.*;
import mindustry.client.ClientVars;
import mindustry.client.utils.ProcessorFinder;
import mindustry.entities.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;

import static mindustry.Vars.*;
import static mindustry.client.ClientVars.*;

public class OverlayRenderer{
    private static final float indicatorLength = 14f;
    private static final float spawnerMargin = tilesize*11f;
    private static final Rect rect = new Rect();

    public float buildFade, unitFade;
    private Sized lastSelect;
    private Seq<CoreEdge> cedges = new Seq<>();
    private boolean updatedCores;

    public OverlayRenderer(){
        Events.on(WorldLoadEvent.class, e -> {
            updatedCores = true;
        });

        Events.on(CoreChangeEvent.class, e -> {
            updatedCores = true;
        });
    }

    private void updateCoreEdges(){
        if(!updatedCores){
            return;
        }

        updatedCores = false;
        cedges.clear();

        Seq<Vec2> pos = new Seq<>();
        Seq<CoreBuild> teams = new Seq<>();
        for(TeamData team : state.teams.active){
            for(CoreBuild b : team.cores){
                teams.add(b);
                pos.add(new Vec2(b.x, b.y));
            }
        }

        if(pos.isEmpty()){
            return;
        }

        //if this is laggy, it could be shoved in another thread.
        var result = Voronoi.generate(pos.toArray(Vec2.class), 0, world.unitWidth(), 0, world.unitHeight());
        for(var edge : result){
            cedges.add(new CoreEdge(edge.x1, edge.y1, edge.x2, edge.y2, teams.get(edge.site1).team, teams.get(edge.site2).team));
        }
    }

    public void drawBottom(){
        if(ClientVars.hidingPlans || player.dead()) return;

//        if (player.isBuilder()) player.unit().drawBuildPlans();
        player.unit().drawBuildPlans();
        drawFrozenPlans();

//        InputHandler input = control.input;
//        input.drawBottom();
        control.input.drawBottom();
    }

    public void drawFrozenPlans(){
        // move frozenPlans.size == 0 out from skip
        if(frozenPlans.size == 0) return;

        // see player.unit().drawBuildPlans();
        var team = player.team();
        var plantopAlpha = 0.24F + Mathf.absin(Time.globalTime, 6.0F, 0.28F);
        Boolf<BuildPlan> skip = plan ->/*plan.progress > 0.01F ||*/ frozenPlans.first() == plan && plan.initialized && (player.unit().within(plan.x * tilesize, plan.y * tilesize, buildingRange) || state.isEditor());

        for (int i = 0; i < 2; i++) {
            for (BuildPlan plan : frozenPlans) {
                if (skip.get(plan)) continue;
                if (i == 0) {
                    //drawPlan
                    plan.animScale = 1.0F;
                    if (plan.breaking) control.input.drawSelected(plan.x, plan.y, plan.block, Pal.freeze);
                    else plan.block.drawPlan(plan, (cons) -> {for(var req:frozenPlans)cons.get(req);}, (Build.validPlace(plan.block, team, plan.x, plan.y, plan.rotation) && Build.validPlaceCoreRange(plan.block, team, plan.x, plan.y)) || control.input.planMatches(plan), 1.0F, true);
                } else {
                    //drawPlanTop
                    Draw.reset();
                    Draw.mixcol(Pal.freeze, plantopAlpha); //TODO: potential optimization here lol
                    Draw.alpha(1.0F);
                    plan.block.drawPlanConfigTop(plan, frozenPlans);
                }
            }
        }
        Draw.reset();
    }

    public void drawTop(){

        if(!player.dead() && ui.hudfrag.shown){
            if(Core.settings.getBool("playerindicators")){
                for(Player player : Groups.player){
                    if(Vars.player != player && Vars.player.team() == player.team()){
                        if(!rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                        .setCenter(Core.camera.position.x, Core.camera.position.y).contains(player.x, player.y)){

                            Tmp.v1.set(player).sub(Vars.player).setLength(indicatorLength);

                            Lines.stroke(2f, Vars.player.team().color);
                            Lines.lineAngle(Vars.player.x + Tmp.v1.x, Vars.player.y + Tmp.v1.y, Tmp.v1.angle(), 4f);
                            Draw.reset();
                        }
                    }
                }
            }

            if(Core.settings.getBool("indicators") && !state.rules.fog){
                Groups.unit.each(unit -> {
                    if(!unit.isLocal() && unit.team != player.team() && !rect.setSize(Core.camera.width * 0.9f, Core.camera.height * 0.9f)
                    .setCenter(Core.camera.position.x, Core.camera.position.y).contains(unit.x, unit.y)){
                        Tmp.v1.set(unit.x, unit.y).sub(player).setLength(indicatorLength);

                        Lines.stroke(1f, unit.team().color);
                        Lines.lineAngle(player.x + Tmp.v1.x, player.y + Tmp.v1.y, Tmp.v1.angle(), 3f);
                        Draw.reset();
                    }
                });
            }
        }

        if(player.dead()) return; //dead players don't draw

        InputHandler input = control.input;

        Sized select = input.selectedUnit();
        if(select == null) select = input.selectedControlBuild();
        if(!Core.input.keyDown(Binding.control) || input.block != null) select = null;

        unitFade = Mathf.lerpDelta(unitFade, Mathf.num(select != null), 0.1f);

        if(select != null) lastSelect = select;
        if(select == null) select = lastSelect;
        if(select != null && (!(select instanceof Unitc u) || u.isAI())){
            boolean showingError = !state.rules.possessionAllowed || (select instanceof Unit u && !u.type.playerControllable);
            Draw.mixcol(showingError ? Pal.remove : Pal.accent, 1f);
            Draw.alpha(unitFade);
            Building build = (select instanceof BlockUnitc b ? b.tile() : select instanceof Building b ? b : null);
            TextureRegion region = build != null ? build.block.fullIcon : Core.atlas.white();

            if(!(select instanceof Unitc)){
                Draw.rect(region, select.getX(), select.getY());
            }

            for(int i = 0; i < 4; i++){
                if(showingError){//Show a warning icon
                    float rot = i * 90f + (20 * Mathf.pow(Mathf.sinDeg(Time.time * 6), 5));
                    //this sin^5 thing causes it to oscillate in a jerky way
                    float length = select.hitSize() * 1.5f + (unitFade * 2.5f);
                    Draw.rect(Icon.warning.getRegion(), select.getX() + Angles.trnsx(rot, length), select.getY() + Angles.trnsy(rot, length), length / 1.9f, length / 1.9f, rot + 90f);
                } else { //Show the unit control arrows
                    float rot = i * 90f + 45f + (-Time.time) % 360f;
                    float length = select.hitSize() * 1.5f + (unitFade * 2.5f);
                    Draw.rect("select-arrow", select.getX() + Angles.trnsx(rot, length), select.getY() + Angles.trnsy(rot, length), length / 1.9f, length / 1.9f, rot - 135f);
                }
            }

            Draw.reset();
        }

        //draw config selected block
        if(input.config.isShown()){
            Building tile = input.config.getSelected();
            tile.drawConfigure();
        }

        input.drawTop();

        buildFade = Mathf.lerpDelta(buildFade, input.isPlacing() || input.isUsingSchematic() ? 1f : 0f, 0.06f);

        Draw.reset();
        Lines.stroke(buildFade * 2f);

        if(buildFade > 0.005f){
            if(state.rules.polygonCoreProtection){
                updateCoreEdges();
                Draw.color(Pal.accent);

                for(int i = 0; i < 2; i++){
                    float offset = (i == 0 ? -2f : 0f);
                    for(CoreEdge edge : cedges){
                        Team displayed = edge.displayed();
                        if(displayed != null){
                            Draw.color(i == 0 ? Color.darkGray : Tmp.c1.set(displayed.color).lerp(Pal.accent, Mathf.absin(Time.time, 10f, 0.2f)));
                            Lines.line(edge.x1, edge.y1 + offset, edge.x2, edge.y2 + offset);
                        }
                    }
                }

                Draw.color();
            }else{
                state.teams.eachEnemyCore(player.team(), core -> {
                    //it must be clear that there is a core here.
                    if(/*core.wasVisible && */Core.camera.bounds(Tmp.r1).overlaps(Tmp.r2.setCentered(core.x, core.y, state.rules.enemyCoreBuildRadius * 2f))){
                        Draw.color(Color.darkGray);
                        Lines.circle(core.x, core.y - 2, state.rules.enemyCoreBuildRadius);
                        Draw.color(Pal.accent, core.team.color, 0.5f + Mathf.absin(Time.time, 10f, 0.5f));
                        Lines.circle(core.x, core.y, state.rules.enemyCoreBuildRadius);
                    }
                });
            }
        }

        Lines.stroke(2f);
        Draw.color(Color.gray, Color.lightGray, Mathf.absin(Time.time, 8f, 1f));

        if(state.hasSpawns() || state.hasSector() && state.getSector().vulnerable()){
            Core.camera.bounds(Tmp.r1);
            boolean isBuilding = input.isBreaking() || input.isPlacing() || input.selectPlans.any() || Core.settings.getBool("alwaysshowdropzone", true);
            float r = state.rules.dropZoneRadius;
            for(Tile tile : spawner.getSpawns()){
                if(tile.within(player.x, player.y, r + spawnerMargin) || (Tmp.r1.overlaps(tile.getX() - r, tile.getY() - r, r * 2, r * 2) && isBuilding)){
                    Draw.alpha(!state.hasSpawns() ? 0.3f : isBuilding ? 1 : Mathf.clamp(1f - (player.dst(tile) - r) / spawnerMargin));
                    Lines.dashCircle(tile.worldx(), tile.worldy(), r);
                }
            }
        }

        Draw.reset();

        //draw selected block
        if(input.block == null && !Core.scene.hasMouse()){
            Vec2 vec = Core.input.mouseWorld();
            Building build = world.buildWorld(vec.x, vec.y);

            if(build != null /* && build.team == player.team() */){
                build.drawSelect();
                if(!build.enabled && build.block.drawDisabled){
                   build.drawDisabled();
                }

                if(Core.input.keyDown(Binding.rotateplaced) && !ui.chatfrag.shown() && build.block.rotate && build.block.quickRotate && build.interactable(player.team())){
                    control.input.drawArrow(build.block, build.tileX(), build.tileY(), build.rotation, true);
                    Draw.color(Pal.accent, 0.3f + Mathf.absin(4f, 0.2f));
                    Fill.square(build.x, build.y, build.block.size * tilesize/2f);
                    Draw.color();
                }
            }
        }

        input.drawOverSelect();

        if(ui.hudfrag.blockfrag.hover() instanceof Unit unit && unit.controller() instanceof LogicAI ai && ai.controller != null && ai.controller.isValid() && !hidingUnits && !(hidingAirUnits && unit.isFlying())){
            var build = ai.controller;
            Drawf.square(build.x, build.y, build.block.size * tilesize/2f + 2f);
            if (Core.settings.getBool("tracelogicunits")) build.drawSelect();
            else if(!unit.within(build, unit.hitSize * 2f)){
                Drawf.arrow(unit.x, unit.y, build.x, build.y, unit.hitSize *2f, 4f);
            }
        }

        //draw selection overlay when dropping item
        if(input.isDroppingItem()){
            Vec2 v = Core.input.mouseWorld(input.getMouseX(), input.getMouseY());
            float size = 8;
            Draw.rect(player.unit().item().fullIcon, v.x, v.y, size, size);
            Draw.color(Pal.accent);
            Lines.circle(v.x, v.y, 6 + Mathf.absin(Time.time, 5f, 1f));
            Draw.reset();

            Building build = world.buildWorld(v.x, v.y);
            if(input.canDropItem() && build != null && build.interactable(player.team()) && build.acceptStack(player.unit().item(), player.unit().stack.amount, player.unit()) > 0 && player.within(build, itemTransferRange) &&
                input.itemDepositCooldown <= 0f){

                boolean invalid = (state.rules.onlyDepositCore && !(build instanceof CoreBuild));

                Lines.stroke(3f, Pal.gray);
                Lines.square(build.x, build.y, build.block.size * tilesize / 2f + 3 + Mathf.absin(Time.time, 5f, 1f));
                Lines.stroke(1f, invalid ? Pal.remove : Pal.place);
                Lines.square(build.x, build.y, build.block.size * tilesize / 2f + 2 + Mathf.absin(Time.time, 5f, 1f));
                Draw.reset();

                if(invalid){
                    build.block.drawPlaceText(Core.bundle.get("bar.onlycoredeposit"), build.tileX(), build.tileY(), false);
                }
            }
        }
        
        ProcessorFinder.INSTANCE.draw();
    }

    public void checkApplySelection(Unit u){
        if(unitFade > 0.001f && lastSelect == u){
            Color prev = Draw.getMixColor();
            Draw.mixcol(prev.a > 0.001f ? prev.lerp(Pal.accent, unitFade) : Pal.accent, Math.max(unitFade, prev.a));
        }
    }

    private static class CoreEdge{
        float x1, y1, x2, y2;
        Team t1, t2;

        public CoreEdge(float x1, float y1, float x2, float y2, Team t1, Team t2){
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.t1 = t1;
            this.t2 = t2;
        }

        @Nullable
        Team displayed(){
            return
                t1 == t2 ? null :
                t1 == player.team() ? t2 :
                t2 == player.team() ? t1 :
                t2.id == 0 ? t1 :
                t1.id < t2.id && t1.id != 0 ? t1 : t2;
        }
    }
}
