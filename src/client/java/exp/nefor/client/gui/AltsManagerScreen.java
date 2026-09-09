package exp.nefor.client.gui;

import exp.nefor.client.config.AltsManager;
import exp.nefor.client.render.RenderSystem;
import exp.nefor.client.util.AnimationUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class AltsManagerScreen extends NeforScreen {

    private static final int PW = 520;
    private static final int PH = 440;

    private final Screen parent;

    private String input = "";
    private boolean inputFocused = true;
    private String selectedAlt;
    private List<String> alts = List.of();
    private List<String> filtered = List.of();
    private String filter = "";
    private double scroll = 0;
    private double targetScroll = 0;
    private long blinkStart = System.currentTimeMillis();
    private float open = 0f;

    public AltsManagerScreen(Screen parent) {
        super(Text.literal("Alt Manager"));
        this.parent = parent;
        refresh();
    }

    @Override public void removed(){ AltsManager.save(); super.removed(); }

    private int px(){ return this.width/2 - PW/2; }
    private int py(){ return Math.max(16, this.height/2 - PH/2); }

    @Override
    public void renderNefor() {
        open = AnimationUtil.animate(open, 1f, 12f);
        targetScroll = Math.max(0, targetScroll);
        scroll = AnimationUtil.lerp((float)scroll,(float)targetScroll,0.22f);

        SakuraBackground.draw(System.currentTimeMillis());
        UiRender.dimScreen(this.width, this.height, 0.52f);

        int x = px(), y = py();
        int animW = (int)(PW* (0.88f+0.12f*open));
        int animH = (int)(PH* (0.88f+0.12f*open));
        int ax = x + (PW-animW)/2; int ay = y + (PH-animH)/2;

        UiRender.roundRect(ax, ay, animW, animH, 12,
                new float[]{0.06f,0.06f,0.11f,0.97f},
                new float[]{1,1,1,0.08f},1,
                new float[]{0.54f,0.17f,0.89f,0.18f},10);

        // header
        RenderSystem.drawText("Alt Manager", ax+18, ay+14, 17f, 0xFF7D9BFF);
        RenderSystem.drawText("offline alts | click to select, double-click to login", ax+18, ay+34, 8f, 0xFF8B8798);
        boolean closeHover = UiRender.inBox(mouseX, mouseY, ax+animW-30, ay+10, 20,20);
        UiRender.roundRect(ax+animW-30, ay+10, 20,20, 6, closeHover? new float[]{1,0.4f,0.4f,0.18f}: new float[]{1,1,1,0.04f}, closeHover? new float[]{1,0.4f,0.4f,0.5f}: new float[]{0,0,0,0},1, new float[]{0,0,0,0},0);
        RenderSystem.drawText("×", ax+animW-24, ay+14, 14f, closeHover? 0xFFFF7070: 0xFF9A9AA6);

        // input + filter
        boolean fieldHover = UiRender.inBox(mouseX, mouseY, ax+18, ay+58, animW-36-108, 28);
        UiRender.roundRect(ax+18, ay+58, animW-36-108, 28, 7,
                new float[]{0.07f,0.07f,0.12f,1f},
                new float[]{1,1,1, inputFocused?0.28f: fieldHover?0.16f:0.08f},1,
                new float[]{0.54f,0.17f,0.89f, inputFocused?0.18f:0f},6);
        String shown = input.isEmpty()? "введи ник..." : input;
        boolean blink = (System.currentTimeMillis()-blinkStart)/530%2==0;
        String render = shown + (inputFocused && blink? "_": "");
        RenderSystem.drawText(render, ax+28, ay+67, 12.5f, input.isEmpty()? 0xFF6A6A74: 0xFFEAEAF2);

        boolean randHover = UiRender.inBox(mouseX, mouseY, ax+animW-18-100, ay+58, 100,28);
        UiRender.roundRect(ax+animW-18-100, ay+58, 100,28, 7, randHover? new float[]{0.35f,0.11f,0.56f,1f}: new float[]{0.12f,0.10f,0.18f,1f}, new float[]{1,1,1,0.08f},1, randHover? new float[]{0.54f,0.17f,0.89f,0.28f}: new float[]{0,0,0,0}, randHover?6:0);
        RenderSystem.drawText("Рандом", ax+animW-18-100+ 24, ay+66, 12f, 0xFFFFFFFF);

        // login button
        boolean loginHover = UiRender.inBox(mouseX, mouseY, ax+18, ay+94, animW-36, 28);
        float pulse = 0.9f+0.1f*(float)Math.sin(System.currentTimeMillis()*0.005);
        UiRender.roundRect(ax+18, ay+94, animW-36, 28, 7,
                loginHover? new float[]{0.36f,0.49f,1f,1f}: new float[]{0.22f,0.18f,0.42f,1f},
                new float[]{1,1,1,0.12f},1,
                new float[]{0.36f,0.49f,1f,0.32f* pulse}, loginHover?8:0);
        RenderSystem.drawText("Войти  →", ax+ animW/2 - RenderSystem.textWidth("Войти  →",13f)/2, ay+102, 13f, 0xFFFFFFFF);

        // filter small
        String filterLabel = filter.isEmpty()? "фильтр..." : filter;
        boolean filterHover = UiRender.inBox(mouseX, mouseY, ax+18, ay+132, 140,18);
        RenderSystem.drawText("Аккаунты ("+filtered.size()+")  "+filterLabel, ax+18, ay+132, 9f, filterHover? 0xFF7D9BFF: 0xFF8B8798);

        int listY = ay+150; int listH = animH-150-52;
        // list bg
        UiRender.roundRect(ax+14, listY, animW-28, listH, 8, new float[]{0.04f,0.04f,0.08f,0.92f}, new float[]{1,1,1,0.05f},1, new float[]{0,0,0,0},0);

        // clip rendering (simple y check)
        int rowH = 32; int visible = listH / rowH;
        int start = (int)(scroll / rowH);
        int yOff = (int)(scroll % rowH);

        int drawn = 0;
        for(int i=start; i<filtered.size() && drawn < visible+1; i++){
            String alt = filtered.get(i);
            int rowY = listY + 6 + drawn*rowH - yOff;
            if(rowY+rowH < listY || rowY > listY+listH) { drawn++; continue; }
            boolean hovered = UiRender.inBox(mouseX, mouseY, ax+20, rowY, animW-40, rowH-4);
            boolean selected = alt.equals(selectedAlt);
            if(selected) UiRender.roundRect(ax+20, rowY, animW-40, rowH-4, 6, new float[]{0.35f,0.11f,0.56f,0.22f}, new float[]{0.36f,0.49f,1f,0.35f},1, new float[]{0,0,0,0},0);
            else if(hovered) UiRender.roundRect(ax+20, rowY, animW-40, rowH-4, 6, new float[]{1,1,1,0.05f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);

            // avatar circle with letter
            UiRender.roundRect(ax+28, rowY+6, 20,20,10, selected? new float[]{0.36f,0.49f,1f,0.9f}: new float[]{1,1,1,0.08f}, new float[]{0,0,0,0},0, selected? new float[]{0.36f,0.49f,1f,0.35f}: new float[]{0,0,0,0}, selected?4:0);
            RenderSystem.drawText(alt.substring(0,1).toUpperCase(), ax+34, rowY+12, 10f, 0xFFFFFFFF);
            RenderSystem.drawText(alt, ax+54, rowY+11, 12f, selected? 0xFF7D9BFF: 0xFFEAEAF2);
            String sub = "offline";
            RenderSystem.drawText(sub, ax+ animW-80, rowY+12, 8f, 0xFF6E6E7A);
            drawn++;
        }
        if(filtered.isEmpty()){
            RenderSystem.drawText(filter.isEmpty()? "пусто — добавь или сгенерируй ник": "ничего не найдено", ax+30, listY+14, 11f, 0xFF6A6A74);
        }
        // scrollbar
        if(filtered.size()*rowH > listH){
            float barH = (float)listH * listH / (filtered.size()*rowH);
            float barY = listY + (float)scroll / (filtered.size()*rowH - listH) * (listH - barH);
            UiRender.roundRect(ax+animW-14, barY, 3, barH, 2, new float[]{1,1,1,0.22f}, new float[]{0,0,0,0},0, new float[]{0,0,0,0},0);
        }

        int btnY = ay+animH-40; int btnW = (animW-36-16)/3;
        // Add
        boolean b1h = UiRender.inBox(mouseX, mouseY, ax+18, btnY, btnW,26);
        UiRender.roundRect(ax+18, btnY, btnW,26,7, b1h? new float[]{0.36f,0.49f,1f,0.18f}: new float[]{1,1,1,0.06f}, new float[]{1,1,1, b1h?0.18f:0.06f},1, new float[]{0,0,0,0},0);
        RenderSystem.drawText("Добавить", ax+18+ btnW/2 - RenderSystem.textWidth("Добавить",11f)/2, btnY+8, 11f, 0xFFEAEAF2);
        // Delete
        boolean b2h = UiRender.inBox(mouseX, mouseY, ax+18+btnW+8, btnY, btnW,26);
        UiRender.roundRect(ax+18+btnW+8, btnY, btnW,26,7, b2h? new float[]{1,0.3f,0.3f,0.16f}: new float[]{1,1,1,0.06f}, new float[]{1,0.3f,0.3f, b2h?0.28f:0.0f},1, new float[]{0,0,0,0},0);
        RenderSystem.drawText("Удалить", ax+18+btnW+8+ btnW/2 - RenderSystem.textWidth("Удалить",11f)/2, btnY+8, 11f, b2h? 0xFFFF8080: 0xFFEAEAF2);
        // Back
        boolean b3h = UiRender.inBox(mouseX, mouseY, ax+18+(btnW+8)*2, btnY, btnW,26);
        UiRender.roundRect(ax+18+(btnW+8)*2, btnY, btnW,26,7, b3h? new float[]{1,1,1,0.10f}: new float[]{0,0,0,0}, new float[]{1,1,1, b3h?0.16f:0.08f},1, new float[]{0,0,0,0},0);
        RenderSystem.drawText("Назад", ax+18+(btnW+8)*2+ btnW/2 - RenderSystem.textWidth("Назад",11f)/2, btnY+8, 11f, 0xFF9A9AA6);

        // hint
        RenderSystem.drawText("Enter - login | wheel - scroll | RMB - copy", ax+18, ay+animH-16, 7.5f, 0xFF6E6E7A);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled){
        double mx=click.x(), my=click.y();
        int x=px(), y=py();
        int ax=x, ay=y; // for hit boxes we use same as render (without anim offset approx)
        int btnW=(PW-36-16)/3; int btnY=y+PH-40; int listY=y+150; int listH=PH-150-52;

        if(click.button()!=0 && click.button()!=1) return super.mouseClicked(click, doubled);

        // close X
        if(UiRender.inBox(mx,my,x+PW-30,y+10,20,20)){ close(); return true; }

        // focus input — НЕ закрываем!
        if(UiRender.inBox(mx,my,x+18,y+58,PW-36-108,28)){
            inputFocused = true;
            blinkStart = System.currentTimeMillis();
            return true;
        }
        // clicking elsewhere unfocus only if not on list/buttons
        // random
        if(UiRender.inBox(mx,my,x+PW-18-100,y+58,100,28)){
            input = AltsManager.generateNick();
            inputFocused=true;
            selectedAlt=null;
            blinkStart=System.currentTimeMillis();
            return true;
        }
        // login
        if(UiRender.inBox(mx,my,x+18,y+94,PW-36,28)){
            String name = !input.isBlank()? input: selectedAlt;
            if(name!=null && !name.isBlank() && AltsManager.login(name)) close();
            else inputFocused=true;
            return true;
        }
        // bottom buttons
        if(UiRender.inBox(mx,my,x+18,btnY,btnW,26)){
            String name = !input.isBlank()? input.trim(): selectedAlt;
            if(name!=null && !name.isEmpty()){ AltsManager.add(name); refresh(); inputFocused=true; }
            return true;
        }
        if(UiRender.inBox(mx,my,x+18+btnW+8,btnY,btnW,26)){
            if(selectedAlt!=null){ AltsManager.remove(selectedAlt); selectedAlt=null; input=""; refresh(); }
            return true;
        }
        if(UiRender.inBox(mx,my,x+18+(btnW+8)*2,btnY,btnW,26)){ close(); return true; }

        // list click
        if(my>=listY && my<listY+listH && mx>=x+20 && mx<=x+PW-20){
            int rowH=32; int start=(int)(scroll/rowH); int yOff=(int)(scroll%rowH);
            int relY = (int)(my - listY -6 + yOff);
            int idx = start + relY / rowH;
            if(idx>=0 && idx<filtered.size()){
                String alt = filtered.get(idx);
                if(doubled){
                    AltsManager.login(alt); close(); return true;
                }
                selectedAlt = alt;
                input = alt;
                inputFocused=true;
                if(click.button()==1){
                    // copy to clipboard via chat hint
                    this.client.keyboard.setClipboard(alt);
                }
                return true;
            } else {
                // click empty area -> unfocus? keep focused
                return true;
            }
        }
        // clicking background should NOT close, only X/back does
        if(UiRender.inBox(mx,my,x,y,PW,PH)){
            return true; // consume to prevent NeforScreen close
        }
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseScrolled(double x,double y,double hx,double vy){
        double mx=x, my=y; // already scaled?
        int listY=py()+150; int listH=PH-150-52;
        if(my>=listY && my<listY+listH){
            targetScroll -= vy*22;
            targetScroll = Math.max(0, Math.min(targetScroll, Math.max(0, filtered.size()*32 - listH)));
            return true;
        }
        return super.mouseScrolled(x,y,hx,vy);
    }

    private void refresh(){ alts = AltsManager.getAlts(); applyFilter(); }
    private void applyFilter(){
        if(filter.isEmpty()) filtered = List.copyOf(alts);
        else filtered = alts.stream().filter(s-> s.toLowerCase().contains(filter.toLowerCase())).collect(Collectors.toList());
        // keep selection visible
        targetScroll = Math.max(0, Math.min(targetScroll, Math.max(0, filtered.size()*32 - (PH-150-52))));
    }

    @Override public boolean charTyped(CharInput ci){
        if(inputFocused && ci.isValidChar() && input.length()<16){
            // allow letters/digits/_
            int cp=ci.codepoint();
            if(Character.isLetterOrDigit(cp) || cp=='_'){
                input+=ci.asString();
                blinkStart=System.currentTimeMillis();
                filter = input;
                applyFilter();
                return true;
            }
        }
        // filter typing when not focused? also update filter
        if(ci.isValidChar()){
            filter+=ci.asString();
            applyFilter();
            return true;
        }
        return super.charTyped(ci);
    }
    @Override public boolean keyPressed(KeyInput ki){
        int k=ki.key();
        if(k==GLFW.GLFW_KEY_ESCAPE){ close(); return true; }
        if(inputFocused){
            if(k==GLFW.GLFW_KEY_BACKSPACE && !input.isEmpty()){
                input=input.substring(0,input.length()-1);
                filter=input;
                applyFilter();
                blinkStart=System.currentTimeMillis();
                return true;
            }
            if(k==GLFW.GLFW_KEY_ENTER){
                String name=!input.isBlank()? input.trim(): selectedAlt;
                if(name!=null && !name.isEmpty() && AltsManager.login(name)) close();
                return true;
            }
            if(k==GLFW.GLFW_KEY_TAB){
                inputFocused=false; return true;
            }
        }
        if(k==GLFW.GLFW_KEY_BACKSPACE && !filter.isEmpty() && !inputFocused){
            filter=filter.substring(0,filter.length()-1); applyFilter(); return true;
        }
        return super.keyPressed(ki);
    }
    @Override public void close(){ AltsManager.save(); if(this.client!=null) this.client.setScreen(parent); }
}
