package exp.nefor.client.gui;

import exp.nefor.client.config.ConfigManager;
import exp.nefor.client.module.api.Module;
import exp.nefor.client.module.ModuleManager;
import exp.nefor.client.module.api.setting.BooleanSetting;
import exp.nefor.client.module.api.setting.ChoiceSetting;
import exp.nefor.client.module.api.setting.KeybindSetting;
import exp.nefor.client.module.api.setting.Setting;
import exp.nefor.client.module.api.setting.SliderSetting;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.AnimationUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import java.util.function.DoubleConsumer;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * ClickGui v2 — красивая структура:
 *  - анимированное открытие/закрытие окна (scale+fade)
 *  - плавный скролл карточек и настроек
 *  - подсветка поиска
 *  - slide-in панели настроек
 *  - мягкие тени, блюр-подложка и hover-анимации
 */
public class ClickGui extends NeforScreen implements Bindable {

    private static final float[] ACCENT = {0.36f, 0.49f, 1.0f};
    private static final int ACCENT_TEXT = 0xFF7D9BFF;
    private static final int TEXT = 0xFFEAEAF2;
    private static final int DIM = 0xFF9A9AA6;
    private static final int LABEL = 0xFF6E6E7A;

    private static final int M = 8;
    private static final int SIDEBAR_W = 150;
    private static final int TOPBAR_H = 42;
    private static final int SETTINGS_W = 210;

    private record Region(double x, double y, double w, double h,
                          Runnable left, Runnable right, Runnable middle,
                          DoubleConsumer drag) {}

    private final List<Region> hits = new ArrayList<>();
    private final Set<String> collapsed = new HashSet<>();
    private final Map<String, Float> categoryAnim = new HashMap<>();
    private final Map<Module, Float> toggleAnim = new HashMap<>();

    private String search = "";
    private boolean searchFocused;
    private String selectedCategory;
    private Module selectedModule;
    private Module animatingSettingsModule;
    private Bindable waitingBind;
    private Region activeDrag;

    private float openProgress = 0f;
    private float settingsProgress = 0f;
    private double scroll = 0;
    private double settingsScroll = 0;
    private double targetScroll = 0;
    private long openTime = System.currentTimeMillis();

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("nefor/clickgui");

    public ClickGui() { super(Text.literal("NeforClient")); }

    @Override
    public void renderNefor() {
        hits.clear();
        float delta = 1/60f;
        openProgress = AnimationUtil.animate(openProgress, 1f, 14f);
        settingsProgress = AnimationUtil.animate(settingsProgress, selectedModule!=null?1f:0f, 12f);
        scroll = AnimationUtil.lerp((float)scroll,(float)targetScroll,0.18f);

        int winW = Math.min(this.width - M * 2, 860);
        int winH = Math.min(this.height - M * 2, 510);
        int winX = (this.width - winW) / 2;
        int winY = (this.height - winH) / 2;
        // scale animation
        float scale = AnimationUtil.easeOutExpo(openProgress);
        int animW = (int)(winW*scale);
        int animH = (int)(winH*scale);
        int animX = winX + (winW-animW)/2;
        int animY = winY + (winH-animH)/2;
        if (openProgress < 0.99f) {
            winX = animX; winY = animY; winW = animW; winH = animH;
        }

        int contentX = winX + SIDEBAR_W;
        boolean settingsOpen = settingsProgress > 0.02f;
        int cardsW = winW - SIDEBAR_W - (int)(SETTINGS_W*settingsProgress);

        // backdrop blur imitation: dim + vignette
        UiRender.dimScreen(this.width, this.height, 0.62f * openProgress);
        // window
        UiRender.roundRect(winX, winY, winW, winH, 12,
                new float[]{0.045f,0.045f,0.065f,0.98f},
                new float[]{1,1,1,0.07f}, 1,
                ACCENT_F(0.09f*openProgress), 12);

        // header logo
        RenderSystem.drawText("N", winX + 16, winY + 11, 23.0f, ACCENT_TEXT);
        RenderSystem.drawText("NEFOR", winX + 34, winY + 10, 13f, 0xFFFFFFFF);
        RenderSystem.drawText("CLIENT  •  1.21.11", winX + 34, winY + 23, 8.5f, LABEL);

        int y = winY + 52;
        RenderSystem.drawText("CATEGORIES", winX + 14, y, 7.5f, LABEL);
        y += 16;
        for (String category : categories()) {
            boolean active = selectedCategory!=null && selectedCategory.equals(category);
            float anim = categoryAnim.getOrDefault(category,0f);
            anim = AnimationUtil.animate(anim, active?1:0, 10f);
            categoryAnim.put(category, anim);

            boolean hovered = UiRender.inBox(mouseX, mouseY, winX+6, y, SIDEBAR_W-12, 24);
            float hover = hovered?1:0;
            if (active || hovered) {
                UiRender.roundRect(winX+6, y, SIDEBAR_W-12, 24, 7,
                        active? ACCENT_F(0.18f) : new float[]{1,1,1,0.05f},
                        active? ACCENT_F(0.35f) : new float[]{0,0,0,0}, 1,
                        new float[]{0,0,0,0},0);
            }
            // left accent bar animated
            if (anim>0.01f) UiRender.roundRect(winX+6, y+4, 2.5f, 16, 1.5f, ACCENT_F(0.85f*anim), new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

            RenderSystem.drawIcon(iconFor(category), winX+12, y+5, 13);
            RenderSystem.drawText(category, winX+30, y+7, 11.5f, active? ACCENT_TEXT : TEXT);
            long count = ModuleManager.getModules().stream().filter(m->m.getCategory().equals(category)).count();
            String cnt = String.valueOf(count);
            float cntW = RenderSystem.textWidth(cnt,8f);
            RenderSystem.drawText(cnt, winX+SIDEBAR_W-16-cntW, y+8, 8f, active? ACCENT_TEXT: LABEL);

            final String cat = category;
            addHit(winX+6, y, SIDEBAR_W-12, 24,
                    ()-> selectedCategory = selectedCategory==null? cat : selectedCategory.equals(cat)? null : cat,
                    null, null);
            y+=26;
        }

        // user card
        String user = this.client.getSession().getUsername();
        int userY = winY+winH-38;
        UiRender.roundRect(winX+8, userY, SIDEBAR_W-16, 30, 8,
                new float[]{1,1,1,0.04f}, new float[]{1,1,1,0.06f},1, new float[]{0,0,0,0},0);
        UiRender.roundRect(winX+13, userY+7, 16,16,8, ACCENT_F(0.40f), new float[]{0,0,0,0},0, ACCENT_F(0.25f),4);
        RenderSystem.drawText(user.substring(0,Math.min(1,user.length())).toUpperCase(), winX+18, userY+11, 9.5f, TEXT);
        RenderSystem.drawText(user, winX+34, userY+7, 10f, TEXT);
        RenderSystem.drawText("Premium", winX+34, userY+17, 8f, LABEL);

        // divider
        UiRender.roundRect(contentX, winY+10, 1, winH-20, 0, new float[]{1,1,1,0.06f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        // search bar
        int searchBarX = contentX+12;
        int searchBarW = cardsW - 24 - 28;
        boolean searchHovered = UiRender.inBox(mouseX, mouseY, searchBarX, winY+10, searchBarW, TOPBAR_H-20);
        UiRender.roundRect(searchBarX, winY+10, searchBarW, TOPBAR_H-20, 8,
                new float[]{1,1,1, searchFocused?0.09f:0.045f},
                new float[]{1,1,1, searchFocused?0.28f:0.07f},1, new float[]{0,0,0,0},0);
        String searchText = search.isEmpty() && !searchFocused? "search modules...": search;
        RenderSystem.drawIcon("search", searchBarX+9, winY+17, 11);
        RenderSystem.drawText(searchText + (searchFocused && (System.currentTimeMillis()/530)%2==0? "_":""),
                searchBarX+26, winY+17, 11f, search.isEmpty()&&!searchFocused? LABEL: TEXT);
        addHit(searchBarX, winY+10, searchBarW, TOPBAR_H-20, ()-> searchFocused=true, null, null);

        // filter icon
        UiRender.roundRect(searchBarX+searchBarW+8, winY+14, 22,22,6,
                UiRender.inBox(mouseX, mouseY, searchBarX+searchBarW+8, winY+14,22,22)? new float[]{1,1,1,0.08f}: new float[]{1,1,1,0.03f},
                new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        for(int i=0;i<3;i++) UiRender.roundRect(searchBarX+searchBarW+13, winY+20+i*4.5, 12,1.8f,1, new float[]{0.78f,0.78f,0.84f,0.9f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        // settings side slide
        if (settingsProgress>0.01f) {
            int sx = winX+winW - (int)(SETTINGS_W*settingsProgress);
            UiRender.roundRect(sx, winY+10, 1, winH-20, 0, new float[]{1,1,1,0.06f*settingsProgress}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
            Module m = selectedModule!=null? selectedModule: animatingSettingsModule;
            if(m!=null){
                if(selectedModule!=null) animatingSettingsModule = selectedModule;
                renderSettingsSide(m, sx+8, winY+TOPBAR_H+4, SETTINGS_W-16);
            }
        }

        // cards area with scroll + scissors imitation via clipping rect manually limited
        if(cardsW>80){
            // enable scroll
            int maxScroll = Math.max(0, estimateCardsHeight(cardsW)- (winH - TOPBAR_H - 16));
            targetScroll = Math.max(0, Math.min(targetScroll, maxScroll));
            renderCards(contentX+12 + (int)scroll*0, winY+TOPBAR_H+4 - (int)scroll, cardsW-24, winH - TOPBAR_H - 16 + (int)scroll);
        }

        // waiting bind overlay
        if(waitingBind!=null){
            String txt = "Press key for " + waitingBind.bindLabel() + "  [ESC to cancel]";
            float w = RenderSystem.textWidth(txt,11f)+20;
            float x = this.width/2f - w/2; float yy = winY+winH+10;
            UiRender.roundRect(x, yy, w, 22, 7, new float[]{0.12f,0.12f,0.18f,0.96f}, ACCENT_F(0.5f),1, ACCENT_F(0.3f),6);
            RenderSystem.drawText(txt, x+10, yy+6, 11f, ACCENT_TEXT);
        }
    }

    private int estimateCardsHeight(int maxW){
        List<Module> visible = visibleModules();
        Map<String,List<Module>> groups = new LinkedHashMap<>();
        for(Module m: visible) groups.computeIfAbsent(m.getCategory(), k->new ArrayList<>()).add(m);
        int cardW=230; int gap=10; int columns=Math.max(1,(maxW+gap)/(cardW+gap));
        int[] colY=new int[columns]; int col=0;
        for(var e: groups.entrySet()){
            int cardH=30 + e.getValue().size()*30 + 6;
            if(colY[col]>0 && colY[col]+cardH> 400 && col<columns-1) col++;
            colY[col]+=cardH+gap;
        }
        int max=0; for(int v: colY) max=Math.max(max,v);
        return max;
    }

    private static float[] ACCENT_F(float alpha){ return new float[]{ACCENT[0],ACCENT[1],ACCENT[2],alpha}; }

    private void addHit(double x,double y,double w,double h,Runnable left,Runnable right,Runnable middle){ hits.add(new Region(x,y,w,h,left,right,middle,null)); }
    private void addDragHit(double x,double y,double w,double h,Runnable left,DoubleConsumer drag){ hits.add(new Region(x,y,w,h,left,null,null,drag)); }

    private void renderCards(int x,int y,int maxW,int maxH){
        List<Module> visible = visibleModules();
        Map<String,List<Module>> groups = new LinkedHashMap<>();
        for(Module module: visible) groups.computeIfAbsent(module.getCategory(), k->new ArrayList<>()).add(module);

        int cardW=230; int gap=10;
        int columns=Math.max(1,(maxW+gap)/(cardW+gap));
        int[] colY=new int[columns]; int col=0;

        for(Map.Entry<String,List<Module>> entry: groups.entrySet()){
            String category=entry.getKey(); List<Module> list=entry.getValue();
            boolean isCollapsed=collapsed.contains(category);
            int cardH=32 + (isCollapsed?0:list.size()*30) + 6;
            if(colY[col]>0 && colY[col]+cardH> maxH && col<columns-1) col++;
            int cx=x+col*(cardW+gap); int cy=y+colY[col]; colY[col]+=cardH+gap;

            // card background with subtle hover
            UiRender.roundRect(cx, cy, cardW, cardH, 10,
                    new float[]{0.078f,0.078f,0.108f,0.96f},
                    new float[]{1,1,1,0.05f},1, new float[]{0,0,0,0},0);
            // header gradient
            UiRender.roundRect(cx, cy, cardW, 30, 10,
                    new float[]{0.09f,0.09f,0.14f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
            RenderSystem.drawIcon(iconFor(category), cx+10, cy+8, 13);
            RenderSystem.drawText(category, cx+28, cy+9, 12.5f, ACCENT_TEXT);
            String badge = list.size()+"";
            float bw = RenderSystem.textWidth(badge,8f)+10;
            UiRender.roundRect(cx+cardW-12-bw, cy+8, bw, 14, 7, new float[]{1,1,1,0.07f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
            RenderSystem.drawText(badge, cx+cardW-12-bw+5, cy+11, 8f, LABEL);
            chevron(cx+cardW-16, cy+15, 8, !isCollapsed);
            addHit(cx+cardW-30, cy, 28,30, ()->{ if(!collapsed.remove(category)) collapsed.add(category);}, null,null);
            if(isCollapsed) continue;

            int rowY=cy+32;
            for(Module module: list){
                renderModuleRow(module, cx+6, rowY, cardW-12);
                rowY+=30;
            }
        }
    }

    private void renderModuleRow(Module module,double x,double y,double w){
        boolean hovered=UiRender.inBox(mouseX,mouseY,x,y,w,28);
        float anim = toggleAnim.getOrDefault(module, module.isEnabled()?1f:0f);
        anim = AnimationUtil.animate(anim, module.isEnabled()?1:0, 12f);
        toggleAnim.put(module, anim);

        if(hovered){
            UiRender.roundRect(x,y,w,28,7, new float[]{1,1,1,0.05f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        }
        // enabled left bar
        if(anim>0.02f) UiRender.roundRect(x, y+4, 2.2, 20,1, ACCENT_F(0.9f*anim), new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        // highlight search
        String name = module.getName();
        float nameX = (float)(x+10);
        if(!search.isEmpty() && name.toLowerCase().contains(search.toLowerCase())){
            UiRender.roundRect(nameX-2, y+5, RenderSystem.textWidth(name,12f)+4, 14,4, ACCENT_F(0.18f), new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        }
        RenderSystem.drawText(name, nameX, (float)(y+8), 12.2f, module.isEnabled()? TEXT: DIM);
        // desc peek
        // dots -> settings
        float dotsX=(float)(x+w-68);
        for(int i=0;i<3;i++) UiRender.roundRect(dotsX+i*6, y+13, 3,3,1.5f, new float[]{0.65f,0.65f,0.72f, hovered?1:0.6f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        addHit(dotsX-4, y, 28,28, ()-> selectedModule= selectedModule==module? null: module, ()-> selectedModule=module, ()-> startBind(module));

        // animated pill
        double pillX = x+w-34; double pillY = y+7;
        double pillW=28; double pillH=14;
        float off = anim*14f;
        UiRender.roundRect(pillX, pillY, pillW,pillH,7, anim>0.5? ACCENT_F(0.95f): new float[]{0.16f,0.16f,0.20f,1f}, new float[]{0,0,0,0},0, anim>0.5? ACCENT_F(0.30f): new float[]{0,0,0,0}, anim>0.5?4:0);
        UiRender.roundRect(pillX+1+off*0.92, pillY+1.5, 11,11,5.5f, new float[]{0.95f,0.96f,1f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

        addHit(pillX-6, y, pillW+12,28, module::toggle, null,null);
        // main row left click toggles, right opens settings
        addHit(x, y, w-80,28, module::toggle, ()-> selectedModule=module, ()-> startBind(module));
    }

    private void renderSettingsSide(Module module,int x,int y,int w){
        List<Setting> settings=module.getSettings();
        int h = 36 + Math.max(1,settings.size())*38 + 10;
        h = Math.min(h, this.height - y - M - 10);
        UiRender.roundRect(x,y,w,h,10, new float[]{0.078f,0.078f,0.108f,0.98f}, new float[]{1,1,1,0.06f},1, ACCENT_F(0.10f),8);
        RenderSystem.drawText(module.getName(), x+10, y+10, 12.5f, TEXT);
        RenderSystem.drawText(module.getDescription(), x+10, y+22, 8f, LABEL);
        RenderSystem.drawText("×", x+w-16, y+9, 14f, UiRender.inBox(mouseX,mouseY,x+w-24,y+6,18,18)? 0xFFFF7070: DIM);
        addHit(x+w-24, y+6, 20,20, ()-> selectedModule=null, null,null);
        int rowY=y+36;
        for(Setting setting: settings){
            boolean hovered=UiRender.inBox(mouseX,mouseY,x+4,rowY,w-8,34);
            if(hovered) UiRender.roundRect(x+4,rowY,w-8,34,7, new float[]{1,1,1,0.045f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
            if(setting instanceof BooleanSetting bool){
                RenderSystem.drawText(setting.getName(), x+8,rowY+8,11f, TEXT);
                double tx=x+w-38; double ty=rowY+10;
                float a = bool.getValue()?1:0;
                UiRender.roundRect(tx,ty,26,14,7, bool.getValue()? ACCENT_F(0.95f): new float[]{0.16f,0.16f,0.20f,1f}, new float[]{0,0,0,0},0, bool.getValue()? ACCENT_F(0.28f): new float[]{0,0,0,0}, bool.getValue()?3:0);
                UiRender.roundRect(tx+1.5 + a*12, ty+1.5, 11,11,5.5f, new float[]{0.95f,0.96f,1f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
                addHit(tx-4,rowY,34,34, bool::toggle,null,null);
            } else if(setting instanceof SliderSetting slider){
                double v=slider.getValue();
                RenderSystem.drawText(setting.getName(), x+8,rowY+2,10.5f, TEXT);
                String vt = v==Math.floor(v)? String.valueOf((int)v): String.format("%.1f",v);
                float vw=RenderSystem.textWidth(vt,10f);
                RenderSystem.drawText(vt, x+w-8-vw, rowY+2,10f, ACCENT_TEXT);
                float trackX=x+8; float trackY=rowY+20; float trackW=w-16;
                double frac=(v-slider.getMin())/(slider.getMax()-slider.getMin());
                UiRender.roundRect(trackX,trackY,trackW,5,2.5f, new float[]{0.17f,0.17f,0.22f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
                float fillW=(float)Math.max(4, trackW*frac);
                UiRender.roundRect(trackX,trackY,fillW,5,2.5f, ACCENT_F(0.95f), new float[]{0,0,0,0},0, ACCENT_F(0.22f),4);
                UiRender.roundRect(trackX+fillW-4, trackY-3, 8,10,4, new float[]{0.95f,0.96f,1f,1f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
                addDragHit(trackX-4,rowY,trackW+8,34,null, dragX->{
                    double f=Math.max(0,Math.min(1,(dragX-trackX)/trackW));
                    slider.setValue(slider.getMin()+f*(slider.getMax()-slider.getMin()));
                });
            } else if(setting instanceof ChoiceSetting choice){
                RenderSystem.drawText(setting.getName(), x+8,rowY+6,10.5f, TEXT);
                String vt="< "+choice.getValue()+" >";
                float vw=RenderSystem.textWidth(vt,10f);
                RenderSystem.drawText(vt, x+w-8-vw, rowY+6,10f, hovered? ACCENT_TEXT: DIM);
                // split left/right
                addHit(x+w/2, rowY, w/2-4,34, ()-> choice.cycle(1), ()-> choice.cycle(-1), null);
                addHit(x+4,rowY,w/2-4,34, ()-> choice.cycle(-1), ()-> choice.cycle(1), null);
            } else if(setting instanceof KeybindSetting keybind){
                RenderSystem.drawText(setting.getName(), x+8,rowY+8,10.5f, TEXT);
                String chip=keyLabel(keybind.getKeyCode()); boolean waiting=waitingBind==keybind;
                float cW=RenderSystem.textWidth(chip,10f)+12;
                UiRender.roundRect(x+w-8-cW, rowY+6, cW,20,6, waiting? ACCENT_F(0.40f): new float[]{0.17f,0.17f,0.22f,0.95f}, waiting? ACCENT_F(0.55f): new float[]{0,0,0,0},1, new float[]{0,0,0,0},0);
                RenderSystem.drawText(chip, x+w-8-cW+6, rowY+11,10f, waiting? ACCENT_TEXT: DIM);
                addHit(x+w-12-cW, rowY, cW+8,34, ()->{ waitingBind=keybind; LOGGER.info("Bind mode (setting): {}", keybind.getName());}, null,null);
            }
            rowY+=38;
        }
        if(settings.isEmpty()) RenderSystem.drawText("Нет настроек", x+8,rowY+6,10f, LABEL);
    }

    private void togglePill(double x,double y,double w,boolean on){ /* unused legacy */ }
    private void chevron(double cx,double cy,double size,boolean down){
        // sleek chevron
        UiRender.roundRect(cx-size/2, cy-1, size,2,1, new float[]{0.70f,0.70f,0.76f, down?0.55f:0.85f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        // arrow head
        double off = down? 2.2: -2.2;
        UiRender.roundRect(cx-3, cy+off, 6,2,1, new float[]{0.70f,0.70f,0.76f, down?0.55f:0.85f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
    }

    private List<Module> visibleModules(){
        List<Module> result=new ArrayList<>(); String q=search.trim().toLowerCase();
        for(Module m: ModuleManager.getModules()){
            if(!q.isEmpty() && !m.getName().toLowerCase().contains(q) && !m.getCategory().toLowerCase().contains(q) && !m.getDescription().toLowerCase().contains(q)) continue;
            if(selectedCategory!=null && !m.getCategory().equals(selectedCategory)) continue;
            result.add(m);
        }
        return result;
    }
    private List<String> categories(){
        List<String> c=new ArrayList<>(); for(Module m: ModuleManager.getModules()) if(!c.contains(m.getCategory())) c.add(m.getCategory()); return c;
    }
    private static String iconFor(String category){
        return switch(category){
            case "Combat" -> "combat";
            case "Movement" -> "movement";
            case "Player" -> "player";
            default -> "misc";
        };
    }

    @Override public boolean mouseClicked(Click click, boolean doubled){
        if(waitingBind!=null){
            int code=click.button(); String label=waitingBind.bindLabel();
            if(code==GLFW.GLFW_MOUSE_BUTTON_1) waitingBind=null;
            else { waitingBind.setKeyCode(code); waitingBind=null; }
            RenderSystem.notification("Bound "+label+" -> "+keyLabel(code), 0xFF7D9BFF);
            return true;
        }
        double mx=click.x(); double my=click.y();
        // unfocus search if clicked outside
        int contentX=M+SIDEBAR_W;
        // rough check for search bar (recalculated)
        if(searchFocused && !UiRender.inBox(mx,my, contentX+12, M+10, 200, 22)) searchFocused=false;
        for(int i=hits.size()-1;i>=0;i--){
            Region r=hits.get(i);
            if(UiRender.inBox(mx,my,r.x(),r.y(),r.w(),r.h())){
                switch(click.button()){
                    case 0->{ if(r.drag()!=null) activeDrag=r; if(r.left()!=null) r.left().run(); }
                    case 1->{ if(r.right()!=null) r.right().run(); }
                    case 2->{ if(r.middle()!=null) r.middle().run(); }
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
    @Override public boolean mouseDragged(Click click,double dx,double dy){
        if(activeDrag!=null && activeDrag.drag()!=null){ activeDrag.drag().accept(click.x()); return true; }
        return super.mouseDragged(click,dx,dy);
    }
    @Override public boolean mouseReleased(Click click){ activeDrag=null; return super.mouseReleased(click); }
    @Override public boolean mouseScrolled(double x,double y,double hx,double vy){
        // scroll cards or settings
        boolean overSettings = selectedModule!=null && mouseX > this.width - SETTINGS_W - 20;
        if(overSettings) settingsScroll += vy*18;
        else targetScroll -= vy*20;
        return true;
    }
    @Override public boolean charTyped(CharInput ci){
        if(searchFocused && ci.isValidChar()){ search+=ci.asString(); targetScroll=0; return true; }
        return super.charTyped(ci);
    }
    @Override public boolean keyPressed(KeyInput ki){
        int kc=ki.key();
        if(waitingBind!=null){
            if(kc==GLFW.GLFW_KEY_ESCAPE){ waitingBind=null; return true; }
            waitingBind.setKeyCode(kc); LOGGER.info("Bound {} -> {} ({})",waitingBind.bindLabel(),kc, GLFW.glfwGetKeyName(kc,0));
            waitingBind=null; return true;
        }
        if(searchFocused){
            if(kc==GLFW.GLFW_KEY_ESCAPE){ searchFocused=false; return true; }
            if(kc==GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()){ search=search.substring(0,search.length()-1); return true; }
            if(kc==GLFW.GLFW_KEY_ENTER){ searchFocused=false; return true; }
            return true;
        }
        if(kc==GLFW.GLFW_KEY_RIGHT_SHIFT || kc==GLFW.GLFW_KEY_ESCAPE){ close(); return true; }
        return super.keyPressed(ki);
    }
    @Override public void close(){
        selectedModule=null; waitingBind=null; search=""; searchFocused=false;
        ConfigManager.save(); super.close();
    }
    private void startBind(Bindable b){ if(waitingBind==b) return; waitingBind=b; LOGGER.info("Bind mode: {}",b.bindLabel()); RenderSystem.notification("Bind: press a key for "+b.bindLabel(), 0xFF7D9BFF); }
    private String keyLabel(int kc){
        if(kc==GLFW.GLFW_KEY_UNKNOWN) return "[ ]";
        if(kc>=GLFW.GLFW_MOUSE_BUTTON_1 && kc<=GLFW.GLFW_MOUSE_BUTTON_5) return "[M"+(kc+1)+"]";
        String name=switch(kc){
            case GLFW.GLFW_KEY_DELETE->"delete"; case GLFW.GLFW_KEY_BACKSPACE->"backspace";
            case GLFW.GLFW_KEY_TAB->"tab"; case GLFW.GLFW_KEY_ENTER->"enter";
            case GLFW.GLFW_KEY_SPACE->"space"; case GLFW.GLFW_KEY_LEFT_SHIFT->"lshift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT->"rshift"; case GLFW.GLFW_KEY_LEFT_CONTROL->"lctrl";
            case GLFW.GLFW_KEY_RIGHT_CONTROL->"rctrl"; default->null;
        };
        if(name==null) name=GLFW.glfwGetKeyName(kc,0);
        return "["+(name==null? "KEY_"+kc: name.toUpperCase())+"]";
    }
    @Override public String bindLabel(){ return "ClickGui"; }
    @Override public int getKeyCode(){ return GLFW.GLFW_KEY_RIGHT_SHIFT; }
    @Override public void setKeyCode(int kc){}
}
